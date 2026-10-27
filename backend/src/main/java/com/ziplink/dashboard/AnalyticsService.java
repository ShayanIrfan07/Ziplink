package com.ziplink.dashboard;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ziplink.common.exception.ApiException;
import com.ziplink.identity.CurrentAccount;
import com.ziplink.url.ShortUrlRepository;
import com.ziplink.url.click.ClickEventRepository;

@Service
public class AnalyticsService {

	static final int DEFAULT_DAYS = 14;
	static final int MAX_DAYS = 90;
	private static final int REFERRER_LIMIT = 5;

	private final ClickEventRepository clicks;
	private final ShortUrlRepository links;
	private final CurrentAccount current;
	private final java.time.Clock clock;

	public AnalyticsService(
			ClickEventRepository clicks,
			ShortUrlRepository links,
			CurrentAccount current,
			java.time.Clock clock) {
		this.clicks = clicks;
		this.links = links;
		this.current = current;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ClickTrendResponse trend(int days, UUID linkId) {
		if (days < 1 || days > MAX_DAYS) {
			throw ApiException.validation("days", "Choose between 1 and 90 days.");
		}
		UUID ownerId = current.requireId();
		if (linkId != null && links.findByIdAndOwnerId(linkId, ownerId).isEmpty()) {
			throw ApiException.notFound("Link not found");
		}
		LocalDate end = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
		LocalDate start = end.minusDays(days - 1L);
		Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant to = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		List<DailyClickResponse> points = ClickSeries.fill(start, end, dailyCounts(ownerId, linkId, from, to));
		long total = points.stream().mapToLong(DailyClickResponse::clicks).sum();
		return new ClickTrendResponse(
				start,
				end,
				total,
				points,
				categories(ownerId, linkId, from, to),
				referrers(ownerId, linkId, from, to));
	}

	private Map<LocalDate, Long> dailyCounts(UUID ownerId, UUID linkId, Instant from, Instant to) {
		List<Object[]> rows = linkId == null
				? clicks.countByUtcDay(ownerId, from, to)
				: clicks.countByUtcDayForLink(ownerId, linkId, from, to);
		Map<LocalDate, Long> counts = new HashMap<>();
		for (Object[] row : rows) {
			counts.put(toLocalDate(row[0]), ((Number) row[1]).longValue());
		}
		return counts;
	}

	private List<CategoryClickResponse> categories(UUID ownerId, UUID linkId, Instant from, Instant to) {
		List<Object[]> rows = linkId == null
				? clicks.countByCategory(ownerId, from, to)
				: clicks.countByCategoryForLink(ownerId, linkId, from, to);
		return rows.stream()
				.map(row -> new CategoryClickResponse(String.valueOf(row[0]), ((Number) row[1]).longValue()))
				.toList();
	}

	private List<ReferrerClickResponse> referrers(UUID ownerId, UUID linkId, Instant from, Instant to) {
		var page = PageRequest.of(0, REFERRER_LIMIT);
		List<Object[]> rows = linkId == null
				? clicks.countByReferrer(ownerId, from, to, page)
				: clicks.countByReferrerForLink(ownerId, linkId, from, to, page);
		return rows.stream()
				.map(row -> new ReferrerClickResponse(row[0] == null ? null : String.valueOf(row[0]), ((Number) row[1]).longValue()))
				.toList();
	}

	private static LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate date) {
			return date;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		throw new IllegalStateException("Unexpected click day value");
	}
}
