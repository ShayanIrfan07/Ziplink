package com.ziplink.url;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.ziplink.guest.Guest;
import com.ziplink.identity.UserAccount;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guest_id")
	private Guest guest;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private UserAccount owner;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Column(name = "short_code", nullable = false, unique = true, length = 32)
	private String shortCode;

	@Column(name = "custom_alias", length = 32)
	private String customAlias;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UrlStatus status;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ShortUrl() {
	}

	public static ShortUrl create(
			Guest guest,
			String originalUrl,
			String shortCode,
			String customAlias,
			Instant expiresAt,
			Instant now) {
		return create(guest, null, originalUrl, shortCode, customAlias, expiresAt, now);
	}

	public static ShortUrl create(
			Guest guest,
			UserAccount owner,
			String originalUrl,
			String shortCode,
			String customAlias,
			Instant expiresAt,
			Instant now) {
		if (guest == null && owner == null) {
			throw new IllegalArgumentException("A link needs a guest or an account");
		}
		ShortUrl url = new ShortUrl();
		url.id = UUID.randomUUID();
		url.guest = guest;
		url.owner = owner;
		url.originalUrl = originalUrl;
		url.shortCode = shortCode;
		url.customAlias = customAlias;
		url.status = UrlStatus.ACTIVE;
		url.expiresAt = expiresAt;
		url.createdAt = now;
		url.updatedAt = now;
		return url;
	}

	public boolean expired(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}

	public void changeDestination(String originalUrl, Instant now) {
		this.originalUrl = originalUrl;
		this.updatedAt = now;
	}

	public void changeStatus(UrlStatus status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}

	public void changeExpiration(Instant expiresAt, Instant now) {
		this.expiresAt = expiresAt;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Guest getGuest() {
		return guest;
	}

	public UserAccount getOwner() {
		return owner;
	}

	public void assignOwner(UserAccount owner, Instant now) {
		this.owner = owner;
		this.guest = null;
		this.updatedAt = now;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public String getShortCode() {
		return shortCode;
	}

	public String getCustomAlias() {
		return customAlias;
	}

	public UrlStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
