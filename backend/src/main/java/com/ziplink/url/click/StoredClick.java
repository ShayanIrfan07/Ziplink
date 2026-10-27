package com.ziplink.url.click;

import java.time.Instant;
import java.util.UUID;

/**
 * A click that passed the bot and prefetch filters. The id makes a repeated
 * delivery of the same event store one row.
 */
public record StoredClick(
		UUID id,
		UUID shortUrlId,
		Instant clickedAt,
		String referrer,
		ClientCategory category) {

	public StoredClick {
		if (id == null || shortUrlId == null || clickedAt == null || category == null) {
			throw new IllegalArgumentException("A stored click needs an id, link, time, and category");
		}
		if (category == ClientCategory.BOT) {
			throw new IllegalArgumentException("Bot clicks are not stored");
		}
	}
}
