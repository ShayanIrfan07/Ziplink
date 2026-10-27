package com.ziplink.guest;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;
import com.ziplink.config.ZiplinkProperties;

@Service
public class GuestService {

	private final GuestRepository repository;
	private final ZiplinkProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public GuestService(GuestRepository repository, ZiplinkProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<Guest> find(HttpServletRequest request) {
		String token = readToken(request);
		if (token == null) {
			return Optional.empty();
		}
		return repository.findByTokenHash(TokenHasher.sha256(token));
	}

	@Transactional
	public Guest resolveOrCreate(HttpServletRequest request, HttpServletResponse response) {
		Optional<Guest> existing = find(request);
		if (existing.isPresent()) {
			return existing.get();
		}
		String token = newToken();
		Guest guest = new Guest(UUID.randomUUID(), TokenHasher.sha256(token), clock.instant());
		repository.save(guest);
		writeCookie(response, token);
		return guest;
	}

	private String readToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, properties.cookie().name());
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
			return null;
		}
		return cookie.getValue();
	}

	private void writeCookie(HttpServletResponse response, String token) {
		ResponseCookie cookie = ResponseCookie.from(properties.cookie().name(), token)
				.httpOnly(true)
				.secure(properties.cookie().secure())
				.sameSite(properties.cookie().sameSite())
				.path("/")
				.maxAge(Duration.ofDays(properties.cookie().maxAgeDays()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
