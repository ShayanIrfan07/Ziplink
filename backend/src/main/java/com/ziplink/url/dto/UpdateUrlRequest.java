package com.ziplink.url.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.ziplink.url.UrlStatus;

public class UpdateUrlRequest {

	private String originalUrl;
	private UrlStatus status;
	private Instant expiresAt;
	private boolean expiresAtPresent;

	public String originalUrl() {
		return originalUrl;
	}

	public UrlStatus status() {
		return status;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	@JsonIgnore
	public boolean expiresAtPresent() {
		return expiresAtPresent;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public void setStatus(UrlStatus status) {
		this.status = status;
	}

	@JsonSetter("expiresAt")
	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
		this.expiresAtPresent = true;
	}
}
