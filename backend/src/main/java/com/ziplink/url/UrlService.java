package com.ziplink.url;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.guest.Guest;
import com.ziplink.identity.UserAccount;
import com.ziplink.url.dto.CreateUrlRequest;
import com.ziplink.url.dto.PageResponse;
import com.ziplink.url.dto.UpdateUrlRequest;
import com.ziplink.url.dto.UrlResponse;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class UrlService {

	static final int MAX_PAGE_SIZE = 100;
	private static final int CODE_ATTEMPTS = 5;
	private static final Set<String> SORT_FIELDS = Set.of("createdAt", "expiresAt", "shortCode");

	private final ShortUrlRepository repository;
	private final ShortUrlWriter writer;
	private final ShortCodeGenerator codes;
	private final UrlDestinationPolicy destinations;
	private final AliasPolicy aliases;
	private final CreateRateLimiter rateLimiter;
	private final ZiplinkProperties properties;
	private final Clock clock;
	private final MeterRegistry meters;
	private final RedirectCache cache;

	public UrlService(
			ShortUrlRepository repository,
			ShortUrlWriter writer,
			ShortCodeGenerator codes,
			UrlDestinationPolicy destinations,
			AliasPolicy aliases,
			CreateRateLimiter rateLimiter,
			ZiplinkProperties properties,
			Clock clock,
			MeterRegistry meters,
			RedirectCache cache) {
		this.repository = repository;
		this.writer = writer;
		this.codes = codes;
		this.destinations = destinations;
		this.aliases = aliases;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.clock = clock;
		this.meters = meters;
		this.cache = cache;
	}

	@Transactional
	public UrlResponse create(Guest guest, CreateUrlRequest request, String clientKey) {
		return create(guest, null, request, clientKey);
	}

	@Transactional
	public UrlResponse createForUser(UserAccount user, CreateUrlRequest request, String clientKey) {
		return create(null, user, request, clientKey);
	}

	@Transactional
	public int claimGuestLinks(UserAccount user, java.util.UUID guestId) {
		return repository.claimGuestLinks(user, guestId, clock.instant());
	}

	private UrlResponse create(Guest guest, UserAccount user, CreateUrlRequest request, String clientKey) {
		rateLimiter.acquire(clientKey);
		String destination = destinations.validate(request.originalUrl());
		Instant expiresAt = request.expiresAt();
		if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
			throw ApiException.validation("expiresAt", "Expiration must be in the future");
		}
		String alias = blankToNull(request.alias());
		String normalizedAlias = alias == null ? null : aliases.normalize(alias);
		if (normalizedAlias != null && repository.existsByShortCodeIgnoreCase(normalizedAlias)) {
			throw ApiException.conflict(ErrorCode.ALIAS_TAKEN, "That alias is already in use");
		}

		ShortUrl saved = null;
		int attempts = normalizedAlias == null ? CODE_ATTEMPTS : 1;
		for (int attempt = 0; attempt < attempts; attempt++) {
			String code = normalizedAlias == null ? codes.randomCode() : normalizedAlias;
			if (normalizedAlias == null && (ReservedCodes.isReserved(code) || repository.existsByShortCodeIgnoreCase(code))) {
				continue;
			}
			try {
				saved = writer.insert(ShortUrl.create(
						guest,
						user,
						destination,
						code,
						normalizedAlias,
						expiresAt,
						clock.instant()));
				break;
			} catch (DataIntegrityViolationException exception) {
				if (normalizedAlias != null) {
					throw ApiException.conflict(ErrorCode.ALIAS_TAKEN, "That alias is already in use");
				}
			}
		}
		if (saved == null) {
			throw new ApiException(
					ErrorCode.INTERNAL_ERROR,
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
					"Could not allocate a short code");
		}
		meters.counter("ziplink.links.created").increment();
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public PageResponse<UrlResponse> list(Guest guest, int page, int size, String sort, String query) {
		return list(guest == null ? null : LinkScope.guest(guest.getId()), page, size, sort, query);
	}

	@Transactional(readOnly = true)
	public PageResponse<UrlResponse> list(LinkScope scope, int page, int size, String sort, String query) {
		if (page < 0) {
			throw ApiException.validation("page", "Page must be zero or greater");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw ApiException.validation("size", "Page size must be between 1 and 100");
		}
		if (query != null && query.length() > 200) {
			throw ApiException.validation("q", "Search text must be at most 200 characters");
		}
		Sort parsed = parseSort(sort);
		if (scope == null) {
			return new PageResponse<>(java.util.List.of(), page, size, 0, 0);
		}
		var specification = scope.userId() != null
				? ShortUrlSpecifications.ownedByUser(scope.userId(), query)
				: ShortUrlSpecifications.ownedBy(scope.guestId(), query);
		Page<ShortUrl> result = repository.findAll(specification, PageRequest.of(page, size, parsed));
		return new PageResponse<>(
				result.stream().map(this::toResponse).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@Transactional(readOnly = true)
	public UrlResponse get(Guest guest, UUID id) {
		return get(LinkScope.guest(guest.getId()), id);
	}

	@Transactional(readOnly = true)
	public UrlResponse get(LinkScope scope, UUID id) {
		return toResponse(owned(scope, id));
	}

	@Transactional
	public UrlResponse update(Guest guest, UUID id, UpdateUrlRequest request) {
		return update(LinkScope.guest(guest.getId()), id, request);
	}

	@Transactional
	public UrlResponse update(LinkScope scope, UUID id, UpdateUrlRequest request) {
		ShortUrl url = owned(scope, id);
		Instant now = clock.instant();
		if (request.originalUrl() != null) {
			url.changeDestination(destinations.validate(request.originalUrl()), now);
		}
		if (request.status() != null) {
			url.changeStatus(request.status(), now);
		}
		if (request.expiresAtPresent()) {
			Instant expiresAt = request.expiresAt();
			if (expiresAt != null && !expiresAt.isAfter(now)) {
				throw ApiException.validation("expiresAt", "Expiration must be in the future");
			}
			url.changeExpiration(expiresAt, now);
		}
		forgetAfterCommit(url.getShortCode());
		return toResponse(url);
	}

	@Transactional
	public void delete(Guest guest, UUID id) {
		delete(LinkScope.guest(guest.getId()), id);
	}

	@Transactional
	public void delete(LinkScope scope, UUID id) {
		ShortUrl url = owned(scope, id);
		repository.delete(url);
		forgetAfterCommit(url.getShortCode());
	}

	@Transactional(readOnly = true)
	public java.util.List<UrlResponse> recentForOwner(UUID userId) {
		return repository.findTop5ByOwnerIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
	}

	/**
	 * Successful redirects use 302 rather than 301. Destinations, status, and
	 * expiration can change, and browsers cache 301 responses aggressively.
	 */
	@Transactional(readOnly = true)
	public RedirectTarget resolve(String shortCode) {
		Optional<RedirectTarget> cached = cache.find(shortCode);
		if (cached.isPresent()) {
			meters.counter("ziplink.redirects", "result", "success").increment();
			return cached.get();
		}
		ShortUrl url = repository.findByShortCodeIgnoreCase(shortCode)
				.orElseThrow(() -> ApiException.notFound("Link not found"));
		if (url.getStatus() != UrlStatus.ACTIVE) {
			throw ApiException.notFound("Link not found");
		}
		if (url.expired(clock.instant())) {
			throw ApiException.gone("This link has expired");
		}
		RedirectTarget target = new RedirectTarget(url.getId(), url.getOriginalUrl());
		cache.remember(url.getShortCode(), target, url.getExpiresAt());
		meters.counter("ziplink.redirects", "result", "success").increment();
		return target;
	}

	private void forgetAfterCommit(String shortCode) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					cache.forget(shortCode);
				}
			});
			return;
		}
		cache.forget(shortCode);
	}

	private ShortUrl owned(LinkScope scope, UUID id) {
		if (scope.userId() != null) {
			return repository.findByIdAndOwnerId(id, scope.userId())
					.orElseThrow(() -> ApiException.notFound("Link not found"));
		}
		return repository.findByIdAndGuestId(id, scope.guestId())
				.orElseThrow(() -> ApiException.notFound("Link not found"));
	}

	private UrlResponse toResponse(ShortUrl url) {
		return new UrlResponse(
				url.getId(),
				url.getOriginalUrl(),
				url.getShortCode(),
				properties.shortUrlFor(url.getShortCode()),
				url.getCustomAlias(),
				url.getStatus(),
				url.expired(clock.instant()),
				url.getExpiresAt(),
				url.getCreatedAt(),
				url.getUpdatedAt());
	}

	private static Sort parseSort(String sort) {
		String value = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
		String[] parts = value.split(",", 2);
		String field = parts[0];
		if (!SORT_FIELDS.contains(field)) {
			throw ApiException.validation("sort", "Sort must be createdAt, expiresAt, or shortCode");
		}
		Sort.Direction direction = Sort.Direction.DESC;
		if (parts.length == 2 && !parts[1].isBlank()) {
			try {
				direction = Sort.Direction.fromString(parts[1].toLowerCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				throw ApiException.validation("sort", "Sort direction must be asc or desc");
			}
		}
		return Sort.by(direction, field);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
