package com.ziplink.dashboard;

import java.util.List;
import com.ziplink.url.dto.UrlResponse;

public record DashboardSummaryResponse(
		long totalLinks,
		long totalClicks,
		long activeLinks,
		long expiredLinks,
		List<UrlResponse> recentLinks,
		List<TopLinkResponse> topLinks,
		List<RecentClickResponse> recentClicks) {
}
