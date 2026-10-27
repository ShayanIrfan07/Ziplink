package com.ziplink.url.click;

import java.time.Clock;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ziplink.url.ShortUrlRepository;

@Service
public class ClickRecorder {

	private final ClickEventRepository clickEvents;
	private final ShortUrlRepository shortUrls;
	private final ClientClassifier classifier;
	private final Clock clock;

	public ClickRecorder(
			ClickEventRepository clickEvents,
			ShortUrlRepository shortUrls,
			ClientClassifier classifier,
			Clock clock) {
		this.clickEvents = clickEvents;
		this.shortUrls = shortUrls;
		this.classifier = classifier;
		this.clock = clock;
	}

	@Async("clickExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(UUID shortUrlId, ClientSignals signals) {
		ClientCategory category = classifier.classify(signals.userAgent());
		if (category == ClientCategory.BOT || classifier.isPrefetch(signals.purpose())) {
			return;
		}
		shortUrls.findById(shortUrlId).ifPresent(shortUrl -> clickEvents.save(new ClickEvent(
				shortUrl,
				clock.instant(),
				classifier.safeReferrer(signals.referrer()),
				category)));
	}
}
