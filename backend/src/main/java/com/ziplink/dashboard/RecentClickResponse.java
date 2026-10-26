package com.ziplink.dashboard;

import java.time.Instant;
import java.util.UUID;

public record RecentClickResponse(
		UUID linkId,
		String shortCode,
		Instant clickedAt,
		String clientCategory,
		String referrer) {
}
