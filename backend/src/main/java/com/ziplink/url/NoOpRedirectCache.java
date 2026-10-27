package com.ziplink.url;

import java.time.Instant;
import java.util.Optional;

public class NoOpRedirectCache implements RedirectCache {

	@Override
	public Optional<RedirectTarget> find(String shortCode) {
		return Optional.empty();
	}

	@Override
	public void remember(String shortCode, RedirectTarget target, Instant expiresAt) {
	}

	@Override
	public void forget(String shortCode) {
	}
}
