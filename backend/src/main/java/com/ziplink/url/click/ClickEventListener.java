package com.ziplink.url.click;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "ziplink.click-events",
		name = {"broker-enabled", "consumer-enabled"},
		havingValue = "true")
public class ClickEventListener {

	private static final Logger log = LoggerFactory.getLogger(ClickEventListener.class);

	private final ClickStore store;

	public ClickEventListener(ClickStore store) {
		this.store = store;
	}

	@KafkaListener(topics = "${ziplink.click-events.topic}", groupId = "${ziplink.click-events.group}")
	public void onClick(String payload) {
		StoredClick click;
		try {
			click = ClickEventCodec.decode(payload);
		} catch (RuntimeException exception) {
			log.warn("Click event was discarded: {}", exception.getClass().getSimpleName());
			return;
		}
		store.save(click);
	}
}
