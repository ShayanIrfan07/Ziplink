package com.ziplink.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;
import com.ziplink.support.RequiresDocker;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "ziplink.clicks-async=false")
@AutoConfigureMockMvc
@Import({com.ziplink.redirect.RedirectController.class, RedirectResolver.class})
@Testcontainers
@ExtendWith(RequiresDocker.class)
class RedirectCacheIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	@DynamicPropertySource
	static void services(org.springframework.test.context.DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		if (!REDIS.isRunning()) {
			REDIS.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("ziplink.cache.enabled", () -> "true");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void redirectKeepsTheCachedDestinationUntilTheLinkChanges() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/urls")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/cached\",\"alias\":\"phase4-cache\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		Cookie guest = cookie(created, "ziplink_guest");
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/phase4-cache").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/cached"));

		jdbc.update("update short_urls set original_url = ? where short_code = ?", "https://example.com/database", "phase4-cache");
		mockMvc.perform(get("/phase4-cache").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/cached"));

		mockMvc.perform(patch("/api/v1/urls/" + id)
						.cookie(guest)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/updated\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/phase4-cache").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/updated"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
		return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
	}

	private static Cookie cookie(MvcResult result, String name) {
		String header = result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(value -> value.startsWith(name + "="))
				.findFirst()
				.orElseThrow();
		String token = header.substring((name + "=").length(), header.indexOf(';'));
		return new Cookie(name, token);
	}
}
