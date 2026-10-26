package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.guest.Guest;
import com.ziplink.url.dto.CreateUrlRequest;
import com.ziplink.url.dto.UpdateUrlRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

	@Mock
	private ShortUrlRepository repository;
	@Mock
	private ShortUrlWriter writer;
	@Mock
	private ShortCodeGenerator codes;

	private UrlService service;
	private Guest guest;

	@BeforeEach
	void setUp() {
		guest = new Guest(UUID.randomUUID(), "a".repeat(64), NOW);
		service = new UrlService(
				repository,
				writer,
				codes,
				new UrlDestinationPolicy(),
				new AliasPolicy(),
				new CreateRateLimiter(100),
				new ZiplinkProperties(
						"http://localhost:8080",
						List.of("http://localhost:3000"),
						new ZiplinkProperties.Cookie("ziplink_guest", false, "Lax", 400),
						new ZiplinkProperties.RateLimit(100),
						false),
				Clock.fixed(NOW, ZoneOffset.UTC),
				new SimpleMeterRegistry());
	}

	@Test
	void createsANormalizedLink() {
		when(codes.randomCode()).thenReturn("abc2345");
		when(writer.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(guest, new CreateUrlRequest(" HTTPS://Example.COM/a ", null, null), "127.0.0.1");

		assertThat(response.shortCode()).isEqualTo("abc2345");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/abc2345");
		assertThat(response.originalUrl()).isEqualTo("https://example.com/a");
		assertThat(response.expired()).isFalse();
	}

	@Test
	void retriesWhenAGeneratedCodeCollides() {
		when(codes.randomCode()).thenReturn("taken11", "free222");
		when(repository.existsByShortCodeIgnoreCase("taken11")).thenReturn(true);
		when(writer.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(guest, new CreateUrlRequest("https://example.com", null, null), "127.0.0.1");

		assertThat(response.shortCode()).isEqualTo("free222");
	}

	@Test
	void translatesAnInsertRaceIntoAnotherAttempt() {
		when(codes.randomCode()).thenReturn("race111", "ok2222x");
		when(writer.insert(any()))
				.thenThrow(new DataIntegrityViolationException("duplicate"))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(guest, new CreateUrlRequest("https://example.com", null, null), "127.0.0.1");

		assertThat(response.shortCode()).isEqualTo("ok2222x");
	}

	@Test
	void rejectsAnAliasThatAlreadyExists() {
		when(repository.existsByShortCodeIgnoreCase("my-link")).thenReturn(true);

		assertThatThrownBy(() -> service.create(
				guest,
				new CreateUrlRequest("https://example.com", "my-link", null),
				"127.0.0.1"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.ALIAS_TAKEN));
		verify(writer, never()).insert(any());
	}

	@Test
	void hidesLinksOwnedBySomeoneElse() {
		when(repository.findByIdAndGuestId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(guest, UUID.randomUUID()))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void expiredLinksAreGoneAndDisabledLinksAreNotFound() {
		ShortUrl expired = ShortUrl.create(
				guest,
				"https://example.com/old",
				"expired1",
				null,
				NOW.minusSeconds(5),
				NOW.minusSeconds(60));
		when(repository.findByShortCodeIgnoreCase("expired1")).thenReturn(Optional.of(expired));
		assertThatThrownBy(() -> service.resolve("expired1"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.status()).isEqualTo(HttpStatus.GONE));

		ShortUrl disabled = ShortUrl.create(
				guest,
				"https://example.com/off",
				"disabled",
				null,
				null,
				NOW.minusSeconds(60));
		disabled.changeStatus(UrlStatus.DISABLED, NOW);
		when(repository.findByShortCodeIgnoreCase("disabled")).thenReturn(Optional.of(disabled));
		assertThatThrownBy(() -> service.resolve("disabled"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void updatesDestinationAndClearsExpiration() {
		ShortUrl existing = ShortUrl.create(
				guest,
				"https://example.com/old",
				"abc2345",
				null,
				NOW.plusSeconds(3600),
				NOW.minusSeconds(60));
		when(repository.findByIdAndGuestId(existing.getId(), guest.getId())).thenReturn(Optional.of(existing));
		UpdateUrlRequest request = new UpdateUrlRequest();
		request.setOriginalUrl("https://example.com/new");
		request.setExpiresAt(null);

		var response = service.update(guest, existing.getId(), request);

		assertThat(response.originalUrl()).isEqualTo("https://example.com/new");
		assertThat(response.expiresAt()).isNull();
	}

	@Test
	void emptyGuestListDoesNotQuery() {
		var page = service.list(null, 0, 20, "createdAt,desc", null);

		assertThat(page.totalElements()).isZero();
		verify(repository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
	}

	@Test
	void rejectsAnUnknownSort() {
		assertThatThrownBy(() -> service.list(guest, 0, 20, "password,desc", null))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	void listsOnlyThroughTheRepositoryPage() {
		ShortUrl existing = ShortUrl.create(guest, "https://example.com", "abc2345", null, null, NOW);
		when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(existing)));

		var page = service.list(guest, 0, 20, "createdAt,desc", "example");

		assertThat(page.items()).hasSize(1);
		assertThat(page.totalElements()).isEqualTo(1);
	}
}
