package com.ziplink.identity;

import java.time.Clock;
import java.util.Optional;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.guest.TokenHasher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Service
public class SessionAuthenticator {

	private final UserSessionRepository sessions;
	private final ZiplinkProperties properties;
	private final Clock clock;

	public SessionAuthenticator(UserSessionRepository sessions, ZiplinkProperties properties, Clock clock) {
		this.sessions = sessions;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<AuthenticatedUser> authenticate(HttpServletRequest request) {
		String token = readToken(request);
		if (token == null) {
			return Optional.empty();
		}
		return sessions.findByTokenHash(TokenHasher.sha256(token))
				.filter(session -> session.getExpiresAt().isAfter(clock.instant()))
				.filter(session -> session.getUser().getStatus() == AccountStatus.ACTIVE)
				.map(session -> new AuthenticatedUser(session.getUser().getId(), session.getUser().getEmail()));
	}

	public void clearCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(properties.session().cookieName(), "")
				.httpOnly(true)
				.secure(properties.cookie().secure())
				.sameSite(properties.cookie().sameSite())
				.path("/")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String readToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, properties.session().cookieName());
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
			return null;
		}
		return cookie.getValue();
	}
}
