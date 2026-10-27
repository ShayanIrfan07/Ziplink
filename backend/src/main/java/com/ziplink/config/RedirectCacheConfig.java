package com.ziplink.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.ziplink.url.NoOpRedirectCache;
import com.ziplink.url.RedisRedirectCache;
import com.ziplink.url.RedirectCache;

@Configuration
public class RedirectCacheConfig {

	@Bean
	@ConditionalOnProperty(prefix = "ziplink.cache", name = "enabled", havingValue = "true")
	RedirectCache redisRedirectCache(StringRedisTemplate redis, ZiplinkProperties properties, java.time.Clock clock) {
		return new RedisRedirectCache(redis, properties, clock);
	}

	@Bean
	@ConditionalOnProperty(prefix = "ziplink.cache", name = "enabled", havingValue = "false")
	RedirectCache noOpRedirectCache() {
		return new NoOpRedirectCache();
	}
}
