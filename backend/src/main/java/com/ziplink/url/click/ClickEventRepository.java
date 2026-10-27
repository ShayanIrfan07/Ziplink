package com.ziplink.url.click;

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
}
