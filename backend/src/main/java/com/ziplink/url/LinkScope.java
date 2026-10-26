package com.ziplink.url;

import java.util.UUID;

public record LinkScope(UUID userId, UUID guestId) {

	public static LinkScope guest(UUID guestId) {
		return new LinkScope(null, guestId);
	}

	public static LinkScope user(UUID userId) {
		return new LinkScope(userId, null);
	}
}
