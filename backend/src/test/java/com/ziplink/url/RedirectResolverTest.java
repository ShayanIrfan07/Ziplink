package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import com.ziplink.common.exception.ApiException;
import com.ziplink.guest.Guest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class RedirectResolverTest {

	private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

	@Mock
	private ShortUrlRepository repository;
	@Mock
	private RedirectCache cache;

	private RedirectResolver resolver;
	private Guest guest;

	@BeforeEach
	void setUp() {
		resolver = new RedirectResolver(
				repository,
				cache,
				Clock.fixed(NOW, ZoneOffset.UTC),
				new SimpleMeterRegistry());
		guest = new Guest(UUID.randomUUID(), "a".repeat(64), NOW);
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
		when(cache.find("expired1")).thenReturn(Optional.empty());
		when(repository.findByShortCodeIgnoreCase("expired1")).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> resolver.resolve("expired1"))
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
		when(cache.find("disabled")).thenReturn(Optional.empty());
		when(repository.findByShortCodeIgnoreCase("disabled")).thenReturn(Optional.of(disabled));

		assertThatThrownBy(() -> resolver.resolve("disabled"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void usesACachedDestinationWithoutReadingTheDatabase() {
		UUID id = UUID.randomUUID();
		when(cache.find("cached1")).thenReturn(Optional.of(new RedirectTarget(id, "https://example.com/cached")));

		RedirectTarget target = resolver.resolve("cached1");

		assertThat(target.destination()).isEqualTo("https://example.com/cached");
		verify(repository, never()).findByShortCodeIgnoreCase(any());
	}

	@Test
	void storesAnActiveLinkAfterACacheMiss() {
		ShortUrl url = ShortUrl.create(guest, "https://example.com/live", "live234", null, null, NOW);
		when(cache.find("live234")).thenReturn(Optional.empty());
		when(repository.findByShortCodeIgnoreCase("live234")).thenReturn(Optional.of(url));

		RedirectTarget target = resolver.resolve("live234");

		assertThat(target.destination()).isEqualTo("https://example.com/live");
		verify(cache).remember(eq("live234"), eq(target), isNull());
	}
}
