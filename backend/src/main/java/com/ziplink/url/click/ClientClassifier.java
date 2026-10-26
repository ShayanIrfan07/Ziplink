package com.ziplink.url.click;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ClientClassifier {

	public ClientCategory classify(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return ClientCategory.UNKNOWN;
		}
		String value = userAgent.toLowerCase(Locale.ROOT);
		if (containsBotMarker(value)) {
			return ClientCategory.BOT;
		}
		if (value.contains("ipad") || value.contains("tablet")) {
			return ClientCategory.TABLET;
		}
		if (value.contains("mobi") || value.contains("iphone") || value.contains("android")) {
			return ClientCategory.MOBILE;
		}
		if (value.contains("windows") || value.contains("macintosh") || value.contains("linux") || value.contains("x11")) {
			return ClientCategory.DESKTOP;
		}
		return ClientCategory.UNKNOWN;
	}

	public boolean isPrefetch(String purpose) {
		return purpose != null && purpose.toLowerCase(Locale.ROOT).contains("prefetch");
	}

	/**
	 * Keeps a referrer usable for reporting without storing query strings, fragments,
	 * or credentials that sometimes appear in the Referer header.
	 */
	public String safeReferrer(String referrer) {
		if (referrer == null || referrer.isBlank()) {
			return null;
		}
		try {
			URI uri = new URI(referrer.trim());
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
				return null;
			}
			if (uri.getHost() == null) {
				return null;
			}
			String path = uri.getRawPath() == null ? "" : uri.getRawPath();
			String value = scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT) + path;
			if (value.length() <= 255) {
				return value;
			}
			String origin = scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT);
			return origin.length() <= 255 ? origin : null;
		} catch (Exception exception) {
			return null;
		}
	}

	private static boolean containsBotMarker(String userAgent) {
		return userAgent.contains("bot")
				|| userAgent.contains("crawl")
				|| userAgent.contains("spider")
				|| userAgent.contains("slurp")
				|| userAgent.contains("preview")
				|| userAgent.contains("wget")
				|| userAgent.contains("curl")
				|| userAgent.contains("httpclient")
				|| userAgent.contains("python-requests")
				|| userAgent.contains("go-http-client")
				|| userAgent.contains("monitor")
				|| userAgent.contains("pingdom")
				|| userAgent.contains("uptimerobot")
				|| userAgent.contains("headless");
	}
}
