package com.ziplink.url;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID>, JpaSpecificationExecutor<ShortUrl> {

	Optional<ShortUrl> findByShortCodeIgnoreCase(String shortCode);

	boolean existsByShortCodeIgnoreCase(String shortCode);

	Optional<ShortUrl> findByIdAndGuestId(UUID id, UUID guestId);

	Optional<ShortUrl> findByIdAndOwnerId(UUID id, UUID ownerId);

	long countByOwnerId(UUID ownerId);

	List<ShortUrl> findTop5ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	@Query("""
			select count(url) from ShortUrl url
			where url.owner.id = :ownerId
			and url.status = com.ziplink.url.UrlStatus.ACTIVE
			and (url.expiresAt is null or url.expiresAt > :now)
			""")
	long countActive(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

	@Query("""
			select count(url) from ShortUrl url
			where url.owner.id = :ownerId
			and url.expiresAt is not null
			and url.expiresAt <= :now
			""")
	long countExpired(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ShortUrl url
			set url.owner = :owner, url.guest = null, url.updatedAt = :now
			where url.guest.id = :guestId and url.owner is null
			""")
	int claimGuestLinks(
			@Param("owner") com.ziplink.identity.UserAccount owner,
			@Param("guestId") UUID guestId,
			@Param("now") Instant now);
}
