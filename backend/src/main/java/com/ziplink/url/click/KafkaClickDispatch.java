package com.ziplink.url.click;

import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public final class KafkaClickDispatch implements ClickDispatch {

	private static final Logger log = LoggerFactory.getLogger(KafkaClickDispatch.class);

	private final KafkaTemplate<String, String> kafka;
	private final String topic;

	public KafkaClickDispatch(KafkaTemplate<String, String> kafka, String topic) {
		this.kafka = kafka;
		this.topic = topic;
	}

	@Override
	public void dispatch(StoredClick click) {
		try {
			kafka.send(topic, click.shortUrlId().toString(), ClickEventCodec.encode(click))
					.whenComplete((result, error) -> {
						if (error != null) {
							Throwable failure = error instanceof CompletionException && error.getCause() != null
									? error.getCause()
									: error;
							log.warn("Click event was not published: {}", failure.getClass().getSimpleName());
						}
					});
		} catch (RuntimeException exception) {
			log.warn("Click event was not published: {}", exception.getClass().getSimpleName());
		}
	}
}
