package com.ziplink.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ziplink.identity.dto.AuthRequest;
import com.ziplink.identity.dto.UserResponse;

@RestController
public class AuthController {

	private final AuthService auth;
	private final CurrentAccount current;

	public AuthController(AuthService auth, CurrentAccount current) {
		this.auth = auth;
		this.current = current;
	}

	@PostMapping("/api/v1/auth/register")
	public ResponseEntity<UserResponse> register(
			@Valid @RequestBody AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		UserResponse user = auth.register(request, httpRequest, httpResponse, clientKey(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(user);
	}

	@PostMapping("/api/v1/auth/login")
	public UserResponse login(
			@Valid @RequestBody AuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return auth.login(request, httpRequest, httpResponse, clientKey(httpRequest));
	}

	@PostMapping("/api/v1/auth/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		auth.logout(request, response);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/v1/auth/me")
	public UserResponse me() {
		return auth.current(current.requireId());
	}

	private static String clientKey(HttpServletRequest request) {
		String address = request.getRemoteAddr();
		return address == null || address.isBlank() ? "unknown" : address;
	}
}
