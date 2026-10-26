package com.ziplink.common.exception;

import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApi(ApiException exception, HttpServletRequest request) {
		if (exception.status().is5xxServerError()) {
			log.error("Request failed with {} at {}", exception.errorCode(), request.getRequestURI());
		} else {
			log.warn("Request rejected with {} at {}", exception.errorCode(), request.getRequestURI());
		}
		if (exception.status() == HttpStatus.TOO_MANY_REQUESTS) {
			return ResponseEntity.status(exception.status())
					.header("Retry-After", "60")
					.body(body(exception.errorCode().name(), exception.getMessage(), exception.fields(), request));
		}
		return ResponseEntity.status(exception.status())
				.body(body(exception.errorCode().name(), exception.getMessage(), exception.fields(), request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ApiError.FieldViolation> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();
		log.warn("Request validation failed at {}", request.getRequestURI());
		return ResponseEntity.badRequest()
				.body(body(ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", fields, request));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
		log.warn("Malformed request body at {}", request.getRequestURI());
		return ResponseEntity.badRequest().body(body(
				ErrorCode.VALIDATION_ERROR.name(),
				"Request body is missing or malformed",
				List.of(),
				request));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
		log.warn("Data integrity violation at {}", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body(
				ErrorCode.CONFLICT.name(),
				"The request conflicts with an existing link",
				List.of(),
				request));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled error at {}", request.getRequestURI(), exception);
		return ResponseEntity.internalServerError().body(body(
				ErrorCode.INTERNAL_ERROR.name(),
				"Something went wrong",
				List.of(),
				request));
	}

	private static ApiError body(
			String code,
			String message,
			List<ApiError.FieldViolation> fields,
			HttpServletRequest request) {
		return new ApiError(code, message, fields, Instant.now(), request.getRequestURI());
	}
}
