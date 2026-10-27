package com.ziplink.url;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public final class RedirectCachePolicy {

	private RedirectCachePolicy() {
	}

	public static String key(String prefix, String shortCode) {
		return prefix + shortCode.toLowerCase(Locale.ROOT);
	}

	public static Duration ttl(Instant now, Instant expiresAt, Duration maximum) {
		if (maximum.isZero() || maximum.isNegative()) {
			return Duration.ZERO;
		}
		if (expiresAt == null) {
			return maximum;
		}
		Duration untilExpiry = Duration.between(now, expiresAt);
		if (untilExpiry.compareTo(Duration.ofSeconds(1)) < 0) {
			return Duration.ZERO;
		}
		return untilExpiry.compareTo(maximum) < 0 ? untilExpiry : maximum;
	}
}
