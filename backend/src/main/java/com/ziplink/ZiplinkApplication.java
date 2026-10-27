package com.ziplink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(
		basePackages = "com.ziplink",
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ziplink\\.worker\\..*")
		})
@ConfigurationPropertiesScan
public class ZiplinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZiplinkApplication.class, args);
	}
}
