package com.ziplink.identity;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest(properties = "ziplink.clicks-async=false")
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(RequiresDocker.class)
class AuthOwnershipIntegrationTest {

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
	void accountsCannotReadEachOthersLinks() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/summary"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ada@example.com\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized());

		MvcResult guestLink = mockMvc.perform(post("/api/v1/urls")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/guest\",\"alias\":\"guest-ada\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		Cookie guest = cookie(guestLink, "ziplink_guest");

		MvcResult ada = mockMvc.perform(post("/api/v1/auth/register")
						.cookie(guest)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"Ada@Example.com\",\"password\":\"password1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();
		Cookie adaSession = cookie(ada, "ziplink_session");
		assertThat(ada.getResponse().getHeaders("Set-Cookie").toString()).contains("HttpOnly");

		mockMvc.perform(get("/api/v1/dashboard/summary").cookie(adaSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalLinks").value(1))
				.andExpect(jsonPath("$.activeLinks").value(1))
				.andExpect(jsonPath("$.totalClicks").value(0));
		mockMvc.perform(get("/api/v1/urls").cookie(guest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		MvcResult created = mockMvc.perform(post("/api/v1/urls")
						.cookie(adaSession)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/private\",\"alias\":\"ada-private\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		MvcResult grace = mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace@example.com\",\"password\":\"password2\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		Cookie graceSession = cookie(grace, "ziplink_session");

		mockMvc.perform(get("/api/v1/urls/" + id).cookie(graceSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/dashboard/summary").cookie(graceSession))
				.andExpect(jsonPath("$.totalLinks").value(0));
		mockMvc.perform(get("/api/v1/urls/" + id).cookie(adaSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("ada-private"));

		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ada@example.com\",\"password\":\"password1\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/auth/logout").cookie(adaSession).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/auth/me").cookie(adaSession))
				.andExpect(status().isUnauthorized());
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
