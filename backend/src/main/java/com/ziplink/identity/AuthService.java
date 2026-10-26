package com.ziplink.identity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.guest.GuestService;
import com.ziplink.guest.TokenHasher;
import com.ziplink.identity.dto.AuthRequest;
import com.ziplink.identity.dto.UserResponse;
import com.ziplink.url.UrlService;

@Service
public class AuthService {

	private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
			.encode("invalid-password");

	private final UserAccountRepository users;
	private final UserSessionRepository sessions;
	private final PasswordEncoder passwordEncoder;
	private final GuestService guests;
	private final UrlService urls;
	private final AuthRateLimiter rateLimiter;
	private final ZiplinkProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public AuthService(
			UserAccountRepository users,
			UserSessionRepository sessions,
			PasswordEncoder passwordEncoder,
			GuestService guests,
			UrlService urls,
			AuthRateLimiter rateLimiter,
			ZiplinkProperties properties,
			Clock clock) {
		this.users = users;
		this.sessions = sessions;
		this.passwordEncoder = passwordEncoder;
		this.guests = guests;
		this.urls = urls;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public UserResponse register(
			AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			String clientKey) {
		rateLimiter.acquire(clientKey);
		String email = normalize(request.email());
		if (users.existsByEmail(email)) {
			throw ApiException.conflict(ErrorCode.EMAIL_TAKEN, "An account with that email already exists");
		}
		UserAccount user = new UserAccount(
				UUID.randomUUID(),
				email,
				passwordEncoder.encode(request.password()),
				clock.instant());
		try {
			users.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict(ErrorCode.EMAIL_TAKEN, "An account with that email already exists");
		}
		claimGuestLinks(httpRequest, user);
		startSession(user, httpResponse);
		return toResponse(user);
	}

	@Transactional
	public UserResponse login(
			AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			String clientKey) {
		rateLimiter.acquire(clientKey);
		String email = normalize(request.email());
		UserAccount user = users.findByEmail(email).orElse(null);
		String hash = user == null ? DUMMY_HASH : user.getPasswordHash();
		boolean matches = passwordEncoder.matches(request.password(), hash);
		if (user == null || !matches || user.getStatus() != AccountStatus.ACTIVE) {
			throw ApiException.unauthorized("Invalid email or password");
		}
		claimGuestLinks(httpRequest, user);
		startSession(user, httpResponse);
		return toResponse(user);
	}

	@Transactional
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		String token = readToken(request);
		if (token != null) {
			sessions.deleteByTokenHash(TokenHasher.sha256(token));
		}
		clearCookie(response);
	}

	@Transactional(readOnly = true)
	public UserResponse current(UUID userId) {
		UserAccount user = users.findById(userId)
				.filter(account -> account.getStatus() == AccountStatus.ACTIVE)
				.orElseThrow(() -> ApiException.unauthorized("Sign in to continue"));
		return toResponse(user);
	}

	private void claimGuestLinks(HttpServletRequest request, UserAccount user) {
		guests.find(request).ifPresent(guest -> urls.claimGuestLinks(user, guest.getId()));
	}

	private void startSession(UserAccount user, HttpServletResponse response) {
		String token = newToken();
		Instant now = clock.instant();
		Instant expiresAt = now.plus(Duration.ofDays(properties.session().maxAgeDays()));
		sessions.save(new UserSession(UUID.randomUUID(), user, TokenHasher.sha256(token), expiresAt, now));
		ResponseCookie cookie = ResponseCookie.from(properties.session().cookieName(), token)
				.httpOnly(true)
				.secure(properties.cookie().secure())
				.sameSite(properties.cookie().sameSite())
				.path("/")
				.maxAge(Duration.ofDays(properties.session().maxAgeDays()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void clearCookie(HttpServletResponse response) {
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

	private String newToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static UserResponse toResponse(UserAccount user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
	}
}
