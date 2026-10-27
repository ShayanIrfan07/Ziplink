package com.ziplink.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ziplink")
public record ZiplinkProperties(
		String publicBaseUrl,
		List<String> corsAllowedOrigins,
		Cookie cookie,
		Session session,
		RateLimit rateLimit,
		boolean clicksAsync,
		Cache cache,
		ClickEvents clickEvents) {

	public ZiplinkProperties {
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			throw new IllegalArgumentException("ziplink.public-base-url is required");
		}
		if (publicBaseUrl.endsWith("/")) {
			publicBaseUrl = publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
		}
		if (!publicBaseUrl.startsWith("http://") && !publicBaseUrl.startsWith("https://")) {
			throw new IllegalArgumentException("ziplink.public-base-url must start with http:// or https://");
		}
		if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty()) {
			throw new IllegalArgumentException("ziplink.cors-allowed-origins is required");
		}
		if (cookie == null || session == null || rateLimit == null) {
			throw new IllegalArgumentException("ziplink cookie, session, and rate-limit settings are required");
		}
		if (session.cookieName() == null || session.cookieName().isBlank() || session.maxAgeDays() < 1) {
			throw new IllegalArgumentException("ziplink.session needs a cookie name and a max age of at least one day");
		}
		if (rateLimit.createPerMinute() < 1 || rateLimit.authPerMinute() < 1) {
			throw new IllegalArgumentException("ziplink rate limits must be at least 1");
		}
		if (cache == null || cache.keyPrefix() == null || cache.keyPrefix().isBlank()) {
			throw new IllegalArgumentException("ziplink.cache needs a key prefix");
		}
		if (cache.ttlSeconds() < 1 || cache.ttlSeconds() > 86_400) {
			throw new IllegalArgumentException("ziplink.cache.ttl-seconds must be between 1 and 86400");
		}
		if (clickEvents == null
				|| clickEvents.topic() == null
				|| !clickEvents.topic().matches("[A-Za-z0-9._-]{1,249}")
				|| clickEvents.group() == null
				|| clickEvents.group().isBlank()
				|| clickEvents.group().length() > 255) {
			throw new IllegalArgumentException("ziplink.click-events needs a topic and a consumer group");
		}
	}

	public String shortUrlFor(String shortCode) {
		return publicBaseUrl + "/" + shortCode;
	}

	public record Cookie(String name, boolean secure, String sameSite, int maxAgeDays) {
	}

	public record Session(String cookieName, int maxAgeDays) {
	}

	public record RateLimit(int createPerMinute, int authPerMinute) {
	}

	public record Cache(boolean enabled, String keyPrefix, int ttlSeconds) {
	}

	public record ClickEvents(boolean brokerEnabled, String topic, String group) {
	}
}
