package com.ziplink.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import com.ziplink.url.click.ClickDispatch;
import com.ziplink.url.click.ClickStore;
import com.ziplink.url.click.KafkaClickDispatch;

@Configuration
@ConditionalOnProperty(
		prefix = "ziplink.click-events",
		name = "publisher-enabled",
		havingValue = "true")
public class ClickEventConfig {

	@Bean
	@ConditionalOnProperty(prefix = "ziplink.click-events", name = "broker-enabled", havingValue = "false")
	ClickDispatch directClickDispatch(ClickStore store) {
		return store::save;
	}

	@Bean
	@ConditionalOnProperty(prefix = "ziplink.click-events", name = "broker-enabled", havingValue = "true")
	ClickDispatch kafkaClickDispatch(KafkaTemplate<String, String> kafka, ZiplinkProperties properties) {
		return new KafkaClickDispatch(kafka, properties.clickEvents().topic());
	}

	@Bean
	@ConditionalOnProperty(prefix = "ziplink.click-events", name = "broker-enabled", havingValue = "true")
	NewTopic clickEventTopic(ZiplinkProperties properties) {
		return TopicBuilder.name(properties.clickEvents().topic()).partitions(1).replicas(-1).build();
	}
}
