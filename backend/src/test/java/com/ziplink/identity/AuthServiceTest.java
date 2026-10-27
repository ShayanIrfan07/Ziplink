package com.ziplink.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.guest.GuestService;
import com.ziplink.identity.dto.AuthRequest;
import com.ziplink.url.UrlService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

	@Mock
	private UserAccountRepository users;
	@Mock
	private UserSessionRepository sessions;
	@Mock
	private GuestService guests;
	@Mock
	private UrlService urls;
	@Mock
	private AuthRateLimiter rateLimiter;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(
				users,
				sessions,
				passwordEncoder,
				guests,
				urls,
				rateLimiter,
				new ZiplinkProperties(
						"http://localhost:8080",
						List.of("http://localhost:3000"),
						new ZiplinkProperties.Cookie("ziplink_guest", false, "Lax", 400),
						new ZiplinkProperties.Session("ziplink_session", 14),
						new ZiplinkProperties.RateLimit(20, 10),
						false,
						new ZiplinkProperties.Cache(false, "ziplink:redirect:", 600)),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void storesAPasswordHashAndSessionCookie() {
		when(users.existsByEmail("ada@example.com")).thenReturn(false);
		when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(guests.find(any())).thenReturn(Optional.empty());
		when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		MockHttpServletResponse response = new MockHttpServletResponse();

		var created = service.register(
				new AuthRequest(" Ada@Example.com ", "password1"),
				new MockHttpServletRequest(),
				response,
				"127.0.0.1");

		assertThat(created.email()).isEqualTo("ada@example.com");
		assertThat(response.getHeader("Set-Cookie")).contains("ziplink_session=").contains("HttpOnly");
		verify(users).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user ->
				!user.getPasswordHash().equals("password1") && passwordEncoder.matches("password1", user.getPasswordHash())));
	}

	@Test
	void rejectsADuplicateEmail() {
		when(users.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.register(
				new AuthRequest("ada@example.com", "password1"),
				new MockHttpServletRequest(),
				new MockHttpServletResponse(),
				"127.0.0.1"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.EMAIL_TAKEN));
		verify(users, never()).saveAndFlush(any());
	}

	@Test
	void rejectsAWrongPassword() {
		UserAccount user = new UserAccount(UUID.randomUUID(), "ada@example.com", passwordEncoder.encode("password1"), NOW);
		when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.login(
				new AuthRequest("ada@example.com", "otherpass"),
				new MockHttpServletRequest(),
				new MockHttpServletResponse(),
				"127.0.0.1"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
		verify(sessions, never()).save(any());
	}
}
