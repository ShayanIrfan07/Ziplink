package com.ziplink.dashboard;

import java.util.UUID;

public record TopLinkResponse(
		UUID id,
		String shortCode,
		String shortUrl,
		String originalUrl,
		long clicks) {
}
