package com.ziplink.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ziplink")
public record ZiplinkProperties(
		String publicBaseUrl,
		List<String> corsAllowedOrigins,
		Cookie cookie,
		RateLimit rateLimit,
		boolean clicksAsync) {

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
		if (cookie == null || rateLimit == null) {
			throw new IllegalArgumentException("ziplink cookie and rate-limit settings are required");
		}
		if (rateLimit.createPerMinute() < 1) {
			throw new IllegalArgumentException("ziplink.rate-limit.create-per-minute must be at least 1");
		}
	}

	public String shortUrlFor(String shortCode) {
		return publicBaseUrl + "/" + shortCode;
	}

	public record Cookie(String name, boolean secure, String sameSite, int maxAgeDays) {
	}

	public record RateLimit(int createPerMinute) {
	}
}
