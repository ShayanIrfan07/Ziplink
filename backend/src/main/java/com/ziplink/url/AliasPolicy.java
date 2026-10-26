package com.ziplink.url;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;

@Component
public class AliasPolicy {

	private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

	public String normalize(String alias) {
		String value = alias == null ? "" : alias.trim();
		if (!PATTERN.matcher(value).matches()) {
			throw ApiException.validation(
					"alias",
					"Alias must be 3-32 characters and use only letters, numbers, hyphens, or underscores");
		}
		if (ReservedCodes.isReserved(value)) {
			throw ApiException.conflict(ErrorCode.ALIAS_RESERVED, "That alias is reserved");
		}
		return value;
	}

	public static String canonical(String value) {
		return value.toLowerCase(Locale.ROOT);
	}
}
