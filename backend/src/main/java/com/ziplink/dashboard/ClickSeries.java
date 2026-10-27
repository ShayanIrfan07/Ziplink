package com.ziplink.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClickSeries {

	private ClickSeries() {
	}

	public static List<DailyClickResponse> fill(LocalDate start, LocalDate end, Map<LocalDate, Long> counts) {
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("End date is before the start date");
		}
		List<DailyClickResponse> points = new ArrayList<>();
		for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
			points.add(new DailyClickResponse(day, counts.getOrDefault(day, 0L)));
		}
		return List.copyOf(points);
	}
}
