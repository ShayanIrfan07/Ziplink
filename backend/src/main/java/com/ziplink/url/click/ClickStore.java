package com.ziplink.url.click;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ziplink.url.ShortUrlRepository;

@Service
public class ClickStore {

	private final ClickEventRepository clickEvents;
	private final ShortUrlRepository shortUrls;
	private final ClientClassifier classifier;

	public ClickStore(ClickEventRepository clickEvents, ShortUrlRepository shortUrls, ClientClassifier classifier) {
		this.clickEvents = clickEvents;
		this.shortUrls = shortUrls;
		this.classifier = classifier;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(StoredClick click) {
		if (clickEvents.existsById(click.id())) {
			return;
		}
		String referrer = classifier.safeReferrer(click.referrer());
		shortUrls.findById(click.shortUrlId()).ifPresent(shortUrl -> clickEvents.save(new ClickEvent(
				click.id(),
				shortUrl,
				click.clickedAt(),
				referrer,
				click.category())));
	}
}
