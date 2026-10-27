package com.ziplink.url.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.ziplink.support.RequiresDocker;
import com.ziplink.worker.ClickWorkerApplication;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "ziplink.clicks-async=false")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(RequiresDocker.class)
class ClickBrokerIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.1");

	@DynamicPropertySource
	static void services(org.springframework.test.context.DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		if (!KAFKA.isRunning()) {
			KAFKA.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
		registry.add("ziplink.click-events.broker-enabled", () -> "true");
		registry.add("ziplink.click-events.consumer-enabled", () -> "false");
		registry.add("ziplink.clicks-async", () -> "false");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClickEventRepository clickEvents;

	@Autowired
	private org.springframework.context.ApplicationContext api;

	@Test
	void browserClicksArriveThroughTheWorkerAndBotsDoNot() throws Exception {
		assertThat(api.getBeansOfType(ClickEventListener.class)).isEmpty();
		try (ConfigurableApplicationContext worker = worker()) {
			assertThat(worker.getBeansOfType(ClickEventListener.class)).hasSize(1);
			storeBrowserClicks();
		}
	}

	private void storeBrowserClicks() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/kafka\",\"alias\":\"phase6-clicks\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/phase6-clicks")
						.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
						.header("Referer", "https://News.Example/story?token=secret#part"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/kafka"));
		waitUntil(1);

		mockMvc.perform(get("/phase6-clicks")
						.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
						.header("Referer", "https://News.Example/story?token=secret#part"))
				.andExpect(status().isFound());
		waitUntil(2);

		mockMvc.perform(get("/phase6-clicks").header("User-Agent", "curl/8.4.0"))
				.andExpect(status().isFound());
		Thread.sleep(Duration.ofSeconds(2));

		assertThat(clickEvents.count()).isEqualTo(2);
		assertThat(clickEvents.findAll()).allSatisfy(event -> {
			assertThat(event.getReferrer()).isEqualTo("https://news.example/story");
			assertThat(event.getClientCategory()).isEqualTo(ClientCategory.DESKTOP);
		});
		assertThat(clickEvents.findAll()).extracting(ClickEvent::getId).doesNotHaveDuplicates();
	}

	private ConfigurableApplicationContext worker() {
		return new SpringApplicationBuilder(ClickWorkerApplication.class)
				.web(WebApplicationType.NONE)
				.run(
						"--spring.main.web-application-type=none",
						"--spring.flyway.enabled=false",
						"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
						"--spring.datasource.username=" + POSTGRES.getUsername(),
						"--spring.datasource.password=" + POSTGRES.getPassword(),
						"--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
						"--ziplink.click-events.broker-enabled=true",
						"--ziplink.click-events.consumer-enabled=true");
	}

	private void waitUntil(long expected) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			if (clickEvents.count() == expected) {
				return;
			}
			Thread.sleep(100);
		}
		assertThat(clickEvents.count()).isEqualTo(expected);
	}
}
