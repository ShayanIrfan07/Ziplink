package com.ziplink.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import com.ziplink.common.exception.ApiException;
import com.ziplink.identity.CurrentAccount;
import com.ziplink.url.ShortUrlRepository;
import com.ziplink.url.click.ClickEventRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

	@Mock
	private ClickEventRepository clicks;
	@Mock
	private ShortUrlRepository links;
	@Mock
	private CurrentAccount current;

	private AnalyticsService analytics;
	private final UUID ownerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		analytics = new AnalyticsService(
				clicks,
				links,
				current,
				Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void fillsTheUtcWindowAndPassesTheQueryBounds() {
		when(current.requireId()).thenReturn(ownerId);
		when(clicks.countByUtcDay(eq(ownerId), any(), any()))
				.thenReturn(List.<Object[]>of(new Object[] {Date.valueOf("2026-10-26"), 2L}));
		when(clicks.countByCategory(eq(ownerId), any(), any()))
				.thenReturn(List.<Object[]>of(new Object[] {"DESKTOP", 2L}));
		when(clicks.countByReferrer(eq(ownerId), any(), any(), any()))
				.thenReturn(List.<Object[]>of(new Object[] {null, 2L}));

		ClickTrendResponse trend = analytics.trend(3, null);

		assertThat(trend.from()).isEqualTo(LocalDate.parse("2026-10-25"));
		assertThat(trend.to()).isEqualTo(LocalDate.parse("2026-10-27"));
		assertThat(trend.totalClicks()).isEqualTo(2);
		assertThat(trend.points()).containsExactly(
				new DailyClickResponse(LocalDate.parse("2026-10-25"), 0),
				new DailyClickResponse(LocalDate.parse("2026-10-26"), 2),
				new DailyClickResponse(LocalDate.parse("2026-10-27"), 0));
		assertThat(trend.categories()).containsExactly(new CategoryClickResponse("DESKTOP", 2));
		assertThat(trend.referrers()).containsExactly(new ReferrerClickResponse(null, 2));

		ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
		verify(clicks).countByUtcDay(eq(ownerId), from.capture(), to.capture());
		assertThat(from.getValue()).isEqualTo(Instant.parse("2026-10-25T00:00:00Z"));
		assertThat(to.getValue()).isEqualTo(Instant.parse("2026-10-28T00:00:00Z"));
		verify(clicks).countByReferrer(eq(ownerId), any(), any(), any(Pageable.class));
	}

	@Test
	void hidesALinkOwnedBySomeoneElse() {
		when(current.requireId()).thenReturn(ownerId);
		UUID linkId = UUID.randomUUID();
		when(links.findByIdAndOwnerId(linkId, ownerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> analytics.trend(14, linkId))
				.isInstanceOf(ApiException.class)
				.hasMessage("Link not found");
		verify(clicks, never()).countByUtcDayForLink(any(), any(), any(), any());
	}

	@Test
	void rejectsARangeOutsideOneToNinetyDays() {
		assertThatThrownBy(() -> analytics.trend(0, null)).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> analytics.trend(91, null)).isInstanceOf(ApiException.class);
		verify(clicks, never()).countByUtcDay(any(), any(), any());
	}
}
