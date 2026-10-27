package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RedirectCachePolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

	@Test
	void usesTheConfiguredTtlWhenTheLinkDoesNotExpire() {
		assertThat(RedirectCachePolicy.ttl(NOW, null, Duration.ofMinutes(10))).isEqualTo(Duration.ofMinutes(10));
	}

	@Test
	void stopsTheEntryAtTheLinkExpiration() {
		assertThat(RedirectCachePolicy.ttl(NOW, NOW.plusSeconds(30), Duration.ofMinutes(10)))
				.isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void doesNotCacheALinkThatExpiresWithinASecond() {
		assertThat(RedirectCachePolicy.ttl(NOW, NOW.plusMillis(500), Duration.ofMinutes(10))).isZero();
	}

	@Test
	void keysAreCaseInsensitive() {
		assertThat(RedirectCachePolicy.key("ziplink:redirect:", "AbC")).isEqualTo("ziplink:redirect:abc");
	}
}
