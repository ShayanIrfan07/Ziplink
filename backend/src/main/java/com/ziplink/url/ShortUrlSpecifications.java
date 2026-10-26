package com.ziplink.url;

import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ShortUrlSpecifications {

	private ShortUrlSpecifications() {
	}

	public static Specification<ShortUrl> ownedBy(java.util.UUID guestId, String query) {
		return (root, criteria, builder) -> {
			var predicates = builder.equal(root.get("guest").get("id"), guestId);
			if (query == null || query.isBlank()) {
				return predicates;
			}
			String pattern = "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
			return builder.and(predicates, builder.or(
					builder.like(builder.lower(root.get("shortCode")), pattern, '\\'),
					builder.like(builder.lower(root.get("originalUrl")), pattern, '\\')));
		};
	}

	static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
