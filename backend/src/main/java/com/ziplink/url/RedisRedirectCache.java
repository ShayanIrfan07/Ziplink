package com.ziplink.url;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.ziplink.config.ZiplinkProperties;

public class RedisRedirectCache implements RedirectCache {

	private static final Logger log = LoggerFactory.getLogger(RedisRedirectCache.class);

	private final StringRedisTemplate redis;
	private final ZiplinkProperties properties;
	private final java.time.Clock clock;

	public RedisRedirectCache(StringRedisTemplate redis, ZiplinkProperties properties, java.time.Clock clock) {
		this.redis = redis;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public Optional<RedirectTarget> find(String shortCode) {
		try {
			Map<Object, Object> fields = redis.opsForHash().entries(key(shortCode));
			if (fields.isEmpty()) {
				return Optional.empty();
			}
			String expiresAt = text(fields.get("expiresAt"));
			if (!expiresAt.isBlank()) {
				Instant expiry = Instant.ofEpochMilli(Long.parseLong(expiresAt));
				if (!expiry.isAfter(clock.instant())) {
					forget(shortCode);
					return Optional.empty();
				}
			}
			return Optional.of(new RedirectTarget(UUID.fromString(text(fields.get("id"))), text(fields.get("destination"))));
		} catch (RuntimeException exception) {
			log.warn("Redirect cache read failed: {}", exception.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	@Override
	public void remember(String shortCode, RedirectTarget target, Instant expiresAt) {
		try {
			Duration ttl = RedirectCachePolicy.ttl(
					clock.instant(),
					expiresAt,
					Duration.ofSeconds(properties.cache().ttlSeconds()));
			if (ttl.compareTo(Duration.ofSeconds(1)) < 0) {
				return;
			}
			String key = key(shortCode);
			redis.opsForHash().putAll(key, Map.of(
					"id", target.id().toString(),
					"destination", target.destination(),
					"expiresAt", expiresAt == null ? "" : Long.toString(expiresAt.toEpochMilli())));
			redis.expire(key, ttl);
		} catch (RuntimeException exception) {
			log.warn("Redirect cache write failed: {}", exception.getClass().getSimpleName());
		}
	}

	@Override
	public void forget(String shortCode) {
		try {
			redis.delete(key(shortCode));
		} catch (RuntimeException exception) {
			log.warn("Redirect cache delete failed: {}", exception.getClass().getSimpleName());
		}
	}

	private String key(String shortCode) {
		return RedirectCachePolicy.key(properties.cache().keyPrefix(), shortCode);
	}

	private static String text(Object value) {
		return value == null ? "" : value.toString();
	}
}
