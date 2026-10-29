package com.ziplink.url;

import java.time.Clock;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import com.ziplink.common.exception.ApiException;
import io.micrometer.core.instrument.MeterRegistry;

public class RedirectResolver {

	private final ShortUrlRepository repository;
	private final RedirectCache cache;
	private final Clock clock;
	private final MeterRegistry meters;

	public RedirectResolver(
			ShortUrlRepository repository,
			RedirectCache cache,
			Clock clock,
			MeterRegistry meters) {
		this.repository = repository;
		this.cache = cache;
		this.clock = clock;
		this.meters = meters;
	}

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
}
