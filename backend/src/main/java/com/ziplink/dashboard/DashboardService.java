package com.ziplink.dashboard;

import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.identity.CurrentAccount;
import com.ziplink.identity.UserAccountRepository;
import com.ziplink.url.ShortUrlRepository;
import com.ziplink.url.UrlService;
import com.ziplink.url.click.ClickEventRepository;

@Service
public class DashboardService {

	private final ShortUrlRepository links;
	private final ClickEventRepository clicks;
	private final UrlService urls;
	private final UserAccountRepository users;
	private final CurrentAccount current;
	private final ZiplinkProperties properties;
	private final Clock clock;

	public DashboardService(
			ShortUrlRepository links,
			ClickEventRepository clicks,
			UrlService urls,
			UserAccountRepository users,
			CurrentAccount current,
			ZiplinkProperties properties,
			Clock clock) {
		this.links = links;
		this.clicks = clicks;
		this.urls = urls;
		this.users = users;
		this.current = current;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse summary() {
		UUID ownerId = current.requireId();
		users.findById(ownerId).orElseThrow(() -> com.ziplink.common.exception.ApiException.unauthorized("Sign in to continue"));
		var now = clock.instant();
		var top = clicks.findTopLinks(ownerId, PageRequest.of(0, 5)).stream()
				.map(row -> new TopLinkResponse(
						row.getId(),
						row.getShortCode(),
						properties.shortUrlFor(row.getShortCode()),
						row.getOriginalUrl(),
						row.getClicks()))
				.toList();
		var recentClicks = clicks.findRecentByOwner(ownerId, PageRequest.of(0, 8)).stream()
				.map(row -> new RecentClickResponse(
						row.getLinkId(),
						row.getShortCode(),
						row.getClickedAt(),
						row.getClientCategory().name(),
						row.getReferrer()))
				.toList();
		return new DashboardSummaryResponse(
				links.countByOwnerId(ownerId),
				clicks.countByOwnerId(ownerId),
				links.countActive(ownerId, now),
				links.countExpired(ownerId, now),
				urls.recentForOwner(ownerId),
				top,
				recentClicks);
	}
}
