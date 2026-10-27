package com.ziplink.identity;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.Cookie;
import org.springframework.web.util.WebUtils;
import com.ziplink.config.ZiplinkProperties;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

	private final SessionAuthenticator authenticator;
	private final ZiplinkProperties properties;

	public SessionAuthenticationFilter(SessionAuthenticator authenticator, ZiplinkProperties properties) {
		this.authenticator = authenticator;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Cookie cookie = WebUtils.getCookie(request, properties.session().cookieName());
		boolean presented = cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank();
		authenticator.authenticate(request).ifPresentOrElse(
				user -> SecurityContextHolder.getContext().setAuthentication(
						UsernamePasswordAuthenticationToken.authenticated(
								user,
								null,
								List.of(new SimpleGrantedAuthority("ROLE_USER")))),
				() -> {
					if (presented) {
						authenticator.clearCookie(response);
					}
				});
		filterChain.doFilter(request, response);
	}
}
