package com.ziplink.dashboard;

import java.time.LocalDate;

public record DailyClickResponse(LocalDate date, long clicks) {
}
