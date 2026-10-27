package com.ziplink.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClickSeriesTest {

	@Test
	void fillsMissingDaysWithZero() {
		LocalDate start = LocalDate.parse("2026-10-25");
		LocalDate end = LocalDate.parse("2026-10-27");

		var points = ClickSeries.fill(start, end, Map.of(LocalDate.parse("2026-10-26"), 2L));

		assertThat(points).containsExactly(
				new DailyClickResponse(start, 0),
				new DailyClickResponse(LocalDate.parse("2026-10-26"), 2),
				new DailyClickResponse(end, 0));
	}

	@Test
	void keepsASingleDay() {
		LocalDate day = LocalDate.parse("2026-10-27");

		assertThat(ClickSeries.fill(day, day, Map.of())).containsExactly(new DailyClickResponse(day, 0));
	}

	@Test
	void rejectsAnEndBeforeTheStart() {
		assertThatThrownBy(() -> ClickSeries.fill(LocalDate.parse("2026-10-27"), LocalDate.parse("2026-10-26"), Map.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
