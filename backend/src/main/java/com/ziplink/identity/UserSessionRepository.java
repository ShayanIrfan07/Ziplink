package com.ziplink.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

	@EntityGraph(attributePaths = "user")
	Optional<UserSession> findByTokenHash(String tokenHash);

	void deleteByTokenHash(String tokenHash);
}
