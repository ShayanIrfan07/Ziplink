package com.ziplink.dashboard;

import java.util.UUID;

public interface TopLinkView {

	UUID getId();

	String getShortCode();

	String getOriginalUrl();

	long getClicks();
}
