package com.ziplink.url.dto;

import java.util.List;

public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
