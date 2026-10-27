package com.ziplink.url;

import java.time.Instant;
import java.util.Optional;

public interface RedirectCache {

	Optional<RedirectTarget> find(String shortCode);

	void remember(String shortCode, RedirectTarget target, Instant expiresAt);

	void forget(String shortCode);
}
