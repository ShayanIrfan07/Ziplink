package com.ziplink.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;
	private final HttpStatus status;
	private final List<ApiError.FieldViolation> fields;

	public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
		this(errorCode, status, message, List.of());
	}

	public ApiException(
			ErrorCode errorCode,
			HttpStatus status,
			String message,
			List<ApiError.FieldViolation> fields) {
		super(message);
		this.errorCode = errorCode;
		this.status = status;
		this.fields = fields;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public HttpStatus status() {
		return status;
	}

	public List<ApiError.FieldViolation> fields() {
		return fields;
	}

	public static ApiException validation(String field, String message) {
		return new ApiException(
				ErrorCode.VALIDATION_ERROR,
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				List.of(new ApiError.FieldViolation(field, message)));
	}

	public static ApiException notFound(String message) {
		return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
	}

	public static ApiException gone(String message) {
		return new ApiException(ErrorCode.GONE, HttpStatus.GONE, message);
	}

	public static ApiException conflict(ErrorCode code, String message) {
		return new ApiException(code, HttpStatus.CONFLICT, message);
	}

	public static ApiException rateLimited() {
		return rateLimited("Too many links were created from this network. Try again in a minute.");
	}

	public static ApiException rateLimited(String message) {
		return new ApiException(ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
	}
}
