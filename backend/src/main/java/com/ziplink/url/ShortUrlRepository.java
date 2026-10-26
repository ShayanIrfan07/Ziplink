package com.ziplink.url;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID>, JpaSpecificationExecutor<ShortUrl> {

	Optional<ShortUrl> findByShortCodeIgnoreCase(String shortCode);

	boolean existsByShortCodeIgnoreCase(String shortCode);

	Optional<ShortUrl> findByIdAndGuestId(UUID id, UUID guestId);
}
