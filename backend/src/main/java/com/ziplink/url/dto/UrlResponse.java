package com.ziplink.url.dto;

import java.time.Instant;
import java.util.UUID;
import com.ziplink.url.UrlStatus;

public record UrlResponse(
		UUID id,
		String originalUrl,
		String shortCode,
		String shortUrl,
		String customAlias,
		UrlStatus status,
		boolean expired,
		Instant expiresAt,
		Instant createdAt,
		Instant updatedAt) {
}
