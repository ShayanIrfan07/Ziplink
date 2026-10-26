package com.ziplink.common.web;

import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9\\-]{8,64}$");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
		String requestId = request.getHeader(HEADER);
		if (requestId == null || !SAFE_ID.matcher(requestId).matches()) {
			requestId = UUID.randomUUID().toString();
		}
		MDC.put("requestId", requestId);
		response.setHeader(HEADER, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("requestId");
		}
	}
}
