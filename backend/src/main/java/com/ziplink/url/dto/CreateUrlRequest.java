package com.ziplink.url.dto;

import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
		@NotBlank(message = "Enter a URL")
		@Size(max = 2048, message = "URL must be at most 2048 characters")
		String originalUrl,
		@Pattern(
				regexp = "^[A-Za-z0-9_-]{3,32}$",
				message = "Alias must be 3-32 characters and use only letters, numbers, hyphens, or underscores")
		String alias,
		@Future(message = "Expiration must be in the future")
		Instant expiresAt) {
}
