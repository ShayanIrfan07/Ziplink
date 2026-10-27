package com.ziplink.url.click;

import java.time.Instant;
import java.util.UUID;

final class ClickEventCodec {

	private static final int MAX_PAYLOAD_CHARS = 4096;

	private ClickEventCodec() {
	}

	static String encode(StoredClick click) {
		String referrer = click.referrer() == null ? "null" : "\"" + escape(click.referrer()) + "\"";
		return "{\"id\":\"" + click.id()
				+ "\",\"shortUrlId\":\"" + click.shortUrlId()
				+ "\",\"clickedAt\":\"" + click.clickedAt()
				+ "\",\"category\":\"" + click.category()
				+ "\",\"referrer\":" + referrer + "}";
	}

	static StoredClick decode(String payload) {
		if (payload == null || payload.isBlank() || payload.length() > MAX_PAYLOAD_CHARS || !payload.startsWith("{")) {
			throw new IllegalArgumentException("Click event payload is not usable");
		}
		try {
			return new StoredClick(
					UUID.fromString(required(payload, "id")),
					UUID.fromString(required(payload, "shortUrlId")),
					Instant.parse(required(payload, "clickedAt")),
					optional(payload, "referrer"),
					ClientCategory.valueOf(required(payload, "category")));
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Click event payload is not usable");
		}
	}

	private static String required(String payload, String key) {
		String value = optional(payload, key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Click event payload is not usable");
		}
		return value;
	}

	private static String optional(String payload, String key) {
		String marker = "\"" + key + "\":";
		int start = payload.indexOf(marker);
		if (start < 0) {
			throw new IllegalArgumentException("Click event payload is not usable");
		}
		int index = start + marker.length();
		while (index < payload.length() && payload.charAt(index) == ' ') {
			index++;
		}
		if (payload.startsWith("null", index)) {
			return null;
		}
		if (index >= payload.length() || payload.charAt(index) != '"') {
			throw new IllegalArgumentException("Click event payload is not usable");
		}
		index++;
		StringBuilder value = new StringBuilder();
		while (index < payload.length()) {
			char current = payload.charAt(index);
			if (current == '"') {
				return value.toString();
			}
			if (current == '\\') {
				index++;
				if (index >= payload.length()) {
					throw new IllegalArgumentException("Click event payload is not usable");
				}
				char escaped = payload.charAt(index);
				switch (escaped) {
					case '"', '\\', '/' -> value.append(escaped);
					case 'n' -> value.append('\n');
					case 'r' -> value.append('\r');
					case 't' -> value.append('\t');
					case 'u' -> {
						if (index + 4 >= payload.length()) {
							throw new IllegalArgumentException("Click event payload is not usable");
						}
						int code = Integer.parseInt(payload.substring(index + 1, index + 5), 16);
						value.append((char) code);
						index += 4;
					}
					default -> throw new IllegalArgumentException("Click event payload is not usable");
				}
			} else if (current < 0x20) {
				throw new IllegalArgumentException("Click event payload is not usable");
			} else {
				value.append(current);
			}
			index++;
		}
		throw new IllegalArgumentException("Click event payload is not usable");
	}

	private static String escape(String value) {
		StringBuilder encoded = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			switch (current) {
				case '"' -> encoded.append("\\\"");
				case '\\' -> encoded.append("\\\\");
				case '\n' -> encoded.append("\\n");
				case '\r' -> encoded.append("\\r");
				case '\t' -> encoded.append("\\t");
				default -> {
					if (current < 0x20) {
						encoded.append(String.format("\\u%04x", (int) current));
					} else {
						encoded.append(current);
					}
				}
			}
		}
		return encoded.toString();
	}
}
