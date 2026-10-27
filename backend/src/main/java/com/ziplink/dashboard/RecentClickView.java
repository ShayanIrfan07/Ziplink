package com.ziplink.dashboard;

import java.time.Instant;
import java.util.UUID;

public interface RecentClickView {

	UUID getLinkId();

	String getShortCode();

	Instant getClickedAt();

	com.ziplink.url.click.ClientCategory getClientCategory();

	String getReferrer();
}
