package com.ziplink.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

	private final DashboardService dashboard;

	public DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping("/api/v1/dashboard/summary")
	public DashboardSummaryResponse summary() {
		return dashboard.summary();
	}
}
