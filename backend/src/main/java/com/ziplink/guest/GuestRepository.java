package com.ziplink.guest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {

	Optional<Guest> findByTokenHash(String tokenHash);
}
