package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.ziplink.support.RequiresDocker;
import com.ziplink.url.click.ClickEventRepository;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "ziplink.clicks.async=false")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(RequiresDocker.class)
class UrlFlowIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@DynamicPropertySource
	static void databaseProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private ClickEventRepository clickEvents;

	@Test
	void createsPersistsAndRedirectsWithoutSharingLinksBetweenGuests() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/docs\"}"))
				.andExpect(status().isForbidden());

		MvcResult created = mockMvc.perform(post("/api/v1/urls")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/docs\",\"alias\":\"docs-link\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").value("docs-link"))
				.andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/docs-link"))
				.andReturn();

		Cookie guest = guestCookie(created);
		mockMvc.perform(get("/docs-link").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/docs"))
				.andExpect(header().string("Cache-Control", "no-store"));
		assertThat(clickEvents.count()).isEqualTo(1);

		mockMvc.perform(get("/docs-link").header("User-Agent", "curl/8.4.0"))
				.andExpect(status().isFound());
		assertThat(clickEvents.count()).isEqualTo(1);

		mockMvc.perform(get("/api/v1/urls").cookie(guest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/v1/urls"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(patch("/api/v1/urls/" + id)
						.cookie(guest)
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"DISABLED\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/docs-link")).andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/urls")
						.cookie(guest)
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/other\",\"alias\":\"docs-link\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/urls")
						.cookie(guest)
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"javascript:alert(1)\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void expiredLinksReturnGone() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/urls")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/soon\",\"alias\":\"soon-link\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		jdbcTemplate.update(
				"update short_urls set expires_at = ? where short_code = ?",
				Timestamp.from(Instant.now().minusSeconds(60)),
				"soon-link");

		mockMvc.perform(get("/soon-link"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("GONE"));
		assertThat(created.getResponse().getStatus()).isEqualTo(201);
	}

	private static Cookie guestCookie(MvcResult result) {
		String header = result.getResponse().getHeader("Set-Cookie");
		assertThat(header).startsWith("ziplink_guest=");
		String token = header.substring("ziplink_guest=".length(), header.indexOf(';'));
		return new Cookie("ziplink_guest", token);
	}
}
