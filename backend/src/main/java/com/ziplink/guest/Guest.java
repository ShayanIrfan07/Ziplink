package com.ziplink.guest;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guests")
public class Guest {

	@Id
	private UUID id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Guest() {
	}

	public Guest(UUID id, String tokenHash, Instant createdAt) {
		this.id = id;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
