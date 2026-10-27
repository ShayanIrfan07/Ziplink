package com.ziplink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI ziplinkOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Ziplink API")
				.version("v1")
				.description("Create short links, manage the links owned by this browser, and redirect visitors."));
	}
}
