package com.ziplink.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;
import com.ziplink.support.RequiresDocker;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "ziplink.clicks.async=false")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(RequiresDocker.class)
class ClickTrendIntegrationTest {

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

	@Test
	void signedInAccountSeesOnlyItsOwnDailyClicks() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/clicks"))
				.andExpect(status().isUnauthorized());

		MvcResult ada = mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"phase3-ada@example.com\",\"password\":\"password1\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		Cookie adaSession = cookie(ada, "ziplink_session");
		MvcResult created = mockMvc.perform(post("/api/v1/urls")
						.cookie(adaSession)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/phase3\",\"alias\":\"phase3-trend\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String linkId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/phase3-trend")
						.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
						.header("Referer", "https://news.example/story?id=1#part"))
				.andExpect(status().isFound());
		mockMvc.perform(get("/phase3-trend").header("User-Agent", "curl/8.4.0"))
				.andExpect(status().isFound());

		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(adaSession).param("days", "14"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.points.length()").value(14))
				.andExpect(jsonPath("$.totalClicks").value(1))
				.andExpect(jsonPath("$.categories[0].category").value("DESKTOP"))
				.andExpect(jsonPath("$.categories[0].clicks").value(1))
				.andExpect(jsonPath("$.referrers[0].referrer").value("https://news.example/story"))
				.andExpect(jsonPath("$.referrers[0].clicks").value(1));

		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(adaSession).param("linkId", linkId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClicks").value(1));

		MvcResult grace = mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"phase3-grace@example.com\",\"password\":\"password2\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		Cookie graceSession = cookie(grace, "ziplink_session");
		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(graceSession).param("linkId", linkId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(graceSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClicks").value(0))
				.andExpect(jsonPath("$.points.length()").value(14))
				.andExpect(jsonPath("$.categories.length()").value(0));

		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(adaSession).param("days", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields[0].field").value("days"));
		mockMvc.perform(get("/api/v1/dashboard/clicks").cookie(adaSession).param("days", "91"))
				.andExpect(status().isBadRequest());
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
