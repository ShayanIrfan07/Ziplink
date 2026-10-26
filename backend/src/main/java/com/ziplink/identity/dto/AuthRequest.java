package com.ziplink.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
		@NotBlank(message = "Enter an email")
		@Email(message = "Enter a valid email")
		@Size(max = 254, message = "Email must be at most 254 characters")
		String email,
		@NotBlank(message = "Enter a password")
		@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
		String password) {
}
