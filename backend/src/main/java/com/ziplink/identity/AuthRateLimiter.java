package com.ziplink.identity;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.ziplink.common.exception.ApiException;
import com.ziplink.config.ZiplinkProperties;

@Component
public class AuthRateLimiter {

	private final int limitPerMinute;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public AuthRateLimiter(ZiplinkProperties properties) {
		this.limitPerMinute = properties.rateLimit().authPerMinute();
	}

	public void acquire(String key) {
		long minute = Instant.now().getEpochSecond() / 60;
		Window window = windows.computeIfAbsent(key, ignored -> new Window());
		synchronized (window) {
			if (window.minute != minute) {
				window.minute = minute;
				window.count = 0;
			}
			if (window.count >= limitPerMinute) {
				throw ApiException.rateLimited(
						"Too many sign-in attempts from this network. Try again in a minute.");
			}
			window.count++;
		}
		if (windows.size() > 10_000) {
			windows.entrySet().removeIf(entry -> entry.getValue().minute != minute);
		}
	}

	private static final class Window {
		private long minute = -1;
		private int count;
	}
}
