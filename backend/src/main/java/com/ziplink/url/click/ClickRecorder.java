package com.ziplink.url.click;

import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
		prefix = "ziplink.click-events",
		name = "publisher-enabled",
		havingValue = "true")
public class ClickRecorder {

	private final ClickDispatch dispatch;
	private final ClientClassifier classifier;
	private final Clock clock;

	public ClickRecorder(ClickDispatch dispatch, ClientClassifier classifier, Clock clock) {
		this.dispatch = dispatch;
		this.classifier = classifier;
		this.clock = clock;
	}

	@Async("clickExecutor")
	public void record(UUID shortUrlId, ClientSignals signals) {
		ClientCategory category = classifier.classify(signals.userAgent());
		if (category == ClientCategory.BOT || classifier.isPrefetch(signals.purpose())) {
			return;
		}
		dispatch.dispatch(new StoredClick(
				UUID.randomUUID(),
				shortUrlId,
				clock.instant(),
				classifier.safeReferrer(signals.referrer()),
				category));
	}
}
