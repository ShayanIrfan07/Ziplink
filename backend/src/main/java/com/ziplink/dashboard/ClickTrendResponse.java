package com.ziplink.dashboard;

import java.time.LocalDate;
import java.util.List;

public record ClickTrendResponse(
		LocalDate from,
		LocalDate to,
		long totalClicks,
		List<DailyClickResponse> points,
		List<CategoryClickResponse> categories,
		List<ReferrerClickResponse> referrers) {
}
