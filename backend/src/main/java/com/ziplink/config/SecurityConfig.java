package com.ziplink.config;

import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ziplink.identity.SessionAuthenticationFilter;
import com.ziplink.identity.SessionAuthenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ZiplinkProperties properties,
			SessionAuthenticator sessions) throws Exception {
		CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfRepository.setCookieCustomizer(cookie -> cookie
				.secure(properties.cookie().secure())
				.sameSite(properties.cookie().sameSite())
				.path("/"));

		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
		csrfHandler.setCsrfRequestAttributeName(null);

		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfRepository)
						.csrfTokenRequestHandler(csrfHandler))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.headers(headers -> headers
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout", "/api/v1/dashboard/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.setCharacterEncoding("UTF-8");
					String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
					response.getWriter().write(
							"{\"code\":\"UNAUTHORIZED\",\"message\":\"Sign in to continue\",\"fields\":[],\"timestamp\":\"%s\",\"path\":\"%s\"}"
									.formatted(Instant.now(), path));
				}))
				.addFilterBefore(new SessionAuthenticationFilter(sessions, properties), AuthorizationFilter.class)
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(ZiplinkProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.corsAllowedOrigins());
		config.setAllowCredentials(true);
		config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of(
				HttpHeaders.CONTENT_TYPE,
				"X-XSRF-TOKEN",
				"X-Request-Id"));
		config.setExposedHeaders(List.of(HttpHeaders.LOCATION, "X-Request-Id"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	/**
	 * Spring Security defers CSRF token generation. Touching the token writes the
	 * readable XSRF-TOKEN cookie the browser sends back on later requests.
	 */
	private static final class CsrfCookieFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(
				HttpServletRequest request,
				HttpServletResponse response,
				FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
			CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (csrfToken != null) {
				csrfToken.getToken();
			}
			filterChain.doFilter(request, response);
		}
	}
}
