package com.ziplink.url.click;

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
import com.ziplink.url.ShortUrl;

@Entity
@Table(name = "click_events")
public class ClickEvent {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "short_url_id", nullable = false)
	private ShortUrl shortUrl;

	@Column(name = "clicked_at", nullable = false)
	private Instant clickedAt;

	@Column(length = 255)
	private String referrer;

	@Enumerated(EnumType.STRING)
	@Column(name = "client_category", nullable = false, length = 16)
	private ClientCategory clientCategory;

	protected ClickEvent() {
	}

	public ClickEvent(ShortUrl shortUrl, Instant clickedAt, String referrer, ClientCategory clientCategory) {
		this.id = UUID.randomUUID();
		this.shortUrl = shortUrl;
		this.clickedAt = clickedAt;
		this.referrer = referrer;
		this.clientCategory = clientCategory;
	}

	public UUID getId() {
		return id;
	}

	public ClientCategory getClientCategory() {
		return clientCategory;
	}

	public String getReferrer() {
		return referrer;
	}
}
