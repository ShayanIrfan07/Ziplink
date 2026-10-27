package com.ziplink.url.click;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ziplink.dashboard.RecentClickView;
import com.ziplink.dashboard.TopLinkView;
import com.ziplink.url.ShortUrl;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

	long countByShortUrlId(UUID shortUrlId);

	@Query("select count(click) from ClickEvent click where click.shortUrl.owner.id = :ownerId")
	long countByOwnerId(@Param("ownerId") UUID ownerId);

	@Query("""
			select url.id as id, url.shortCode as shortCode, url.originalUrl as originalUrl, count(click.id) as clicks
			from ShortUrl url
			left join ClickEvent click on click.shortUrl = url
			where url.owner.id = :ownerId
			group by url.id, url.shortCode, url.originalUrl
			having count(click.id) > 0
			order by count(click.id) desc, url.shortCode asc
			""")
	List<TopLinkView> findTopLinks(@Param("ownerId") UUID ownerId, Pageable pageable);

	@Query("""
			select url.id as linkId, url.shortCode as shortCode, click.clickedAt as clickedAt,
			       click.clientCategory as clientCategory, click.referrer as referrer
			from ClickEvent click
			join click.shortUrl url
			where url.owner.id = :ownerId
			order by click.clickedAt desc
			""")
	List<RecentClickView> findRecentByOwner(@Param("ownerId") UUID ownerId, Pageable pageable);

	@Query(value = """
			SELECT CAST(click.clicked_at AT TIME ZONE 'UTC' AS date) AS bucket,
			       COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY CAST(click.clicked_at AT TIME ZONE 'UTC' AS date)
			ORDER BY CAST(click.clicked_at AT TIME ZONE 'UTC' AS date)
			""", nativeQuery = true)
	List<Object[]> countByUtcDay(
			@Param("ownerId") UUID ownerId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query(value = """
			SELECT CAST(click.clicked_at AT TIME ZONE 'UTC' AS date) AS bucket,
			       COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND url.id = :linkId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY CAST(click.clicked_at AT TIME ZONE 'UTC' AS date)
			ORDER BY CAST(click.clicked_at AT TIME ZONE 'UTC' AS date)
			""", nativeQuery = true)
	List<Object[]> countByUtcDayForLink(
			@Param("ownerId") UUID ownerId,
			@Param("linkId") UUID linkId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query(value = """
			SELECT click.client_category AS category, COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY click.client_category
			ORDER BY COUNT(*) DESC, click.client_category ASC
			""", nativeQuery = true)
	List<Object[]> countByCategory(
			@Param("ownerId") UUID ownerId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query(value = """
			SELECT click.client_category AS category, COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND url.id = :linkId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY click.client_category
			ORDER BY COUNT(*) DESC, click.client_category ASC
			""", nativeQuery = true)
	List<Object[]> countByCategoryForLink(
			@Param("ownerId") UUID ownerId,
			@Param("linkId") UUID linkId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query(value = """
			SELECT click.referrer AS referrer, COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY click.referrer
			ORDER BY COUNT(*) DESC, click.referrer ASC NULLS LAST
			""", nativeQuery = true)
	List<Object[]> countByReferrer(
			@Param("ownerId") UUID ownerId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);

	@Query(value = """
			SELECT click.referrer AS referrer, COUNT(*) AS clicks
			FROM click_events click
			JOIN short_urls url ON url.id = click.short_url_id
			WHERE url.owner_id = :ownerId
			  AND url.id = :linkId
			  AND click.clicked_at >= :from
			  AND click.clicked_at < :to
			GROUP BY click.referrer
			ORDER BY COUNT(*) DESC, click.referrer ASC NULLS LAST
			""", nativeQuery = true)
	List<Object[]> countByReferrerForLink(
			@Param("ownerId") UUID ownerId,
			@Param("linkId") UUID linkId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);
}
