package com.ziplink.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
		String code,
		String message,
		List<FieldViolation> fields,
		Instant timestamp,
		String path) {

	public ApiError {
		fields = fields == null ? List.of() : List.copyOf(fields);
	}

	public record FieldViolation(String field, String message) {
	}
}
