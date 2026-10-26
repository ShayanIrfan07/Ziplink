package com.ziplink.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.ziplink.common.exception.ApiException;

@Component
public class UrlDestinationPolicy {

	public static final int MAX_LENGTH = 2048;

	public String validate(String raw) {
		if (raw == null || raw.isBlank()) {
			throw ApiException.validation("originalUrl", "Enter a URL");
		}
		String trimmed = raw.trim();
		if (trimmed.length() > MAX_LENGTH) {
			throw ApiException.validation("originalUrl", "URL must be at most 2048 characters");
		}
		if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\0') >= 0) {
			throw ApiException.validation("originalUrl", "URL contains invalid characters");
		}
		URI uri;
		try {
			uri = new URI(trimmed);
		} catch (URISyntaxException exception) {
			throw ApiException.validation("originalUrl", "Enter a valid http or https URL");
		}
		String scheme = uri.getScheme();
		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw ApiException.validation("originalUrl", "Only http and https URLs are allowed");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw ApiException.validation("originalUrl", "URL must include a host");
		}
		if (uri.getRawUserInfo() != null) {
			throw ApiException.validation("originalUrl", "URLs with embedded credentials are not allowed");
		}
		return rebuild(uri, scheme);
	}

	private static String rebuild(URI uri, String scheme) {
		String path = uri.getRawPath() == null ? "" : uri.getRawPath();
		StringBuilder builder = new StringBuilder();
		builder.append(scheme.toLowerCase(Locale.ROOT))
				.append("://")
				.append(uri.getHost().toLowerCase(Locale.ROOT));
		if (uri.getPort() != -1) {
			builder.append(':').append(uri.getPort());
		}
		builder.append(path);
		if (uri.getRawQuery() != null) {
			builder.append('?').append(uri.getRawQuery());
		}
		if (uri.getRawFragment() != null) {
			builder.append('#').append(uri.getRawFragment());
		}
		return builder.toString();
	}
}
