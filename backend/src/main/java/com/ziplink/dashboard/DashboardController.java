package com.ziplink.dashboard;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

	private final DashboardService dashboard;
	private final AnalyticsService analytics;

	public DashboardController(DashboardService dashboard, AnalyticsService analytics) {
		this.dashboard = dashboard;
		this.analytics = analytics;
	}

	@GetMapping("/api/v1/dashboard/summary")
	public DashboardSummaryResponse summary() {
		return dashboard.summary();
	}

	@GetMapping("/api/v1/dashboard/clicks")
	public ClickTrendResponse clicks(
			@RequestParam(defaultValue = "14") int days,
			@RequestParam(required = false) UUID linkId) {
		return analytics.trend(days, linkId);
	}
}
