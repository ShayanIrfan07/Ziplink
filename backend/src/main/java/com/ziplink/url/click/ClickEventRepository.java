package com.ziplink.url.click;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

	long countByShortUrlId(UUID shortUrlId);
}
