package com.ziplink.url;

import java.util.Locale;
import java.util.Set;

public final class ReservedCodes {

	private static final Set<String> CODES = Set.of(
			"api",
			"v1",
			"auth",
			"urls",
			"dashboard",
			"analytics",
			"actuator",
			"health",
			"info",
			"metrics",
			"swagger-ui",
			"swagger-ui.html",
			"v3",
			"error",
			"favicon.ico",
			"favicon",
			"robots.txt",
			"login",
			"register",
			"logout",
			"me",
			"links",
			"admin",
			"static",
			"assets",
			"public",
			"_next",
			"graphql",
			"openapi");

	private ReservedCodes() {
	}

	public static boolean isReserved(String code) {
		return code != null && CODES.contains(code.toLowerCase(Locale.ROOT));
	}
}
