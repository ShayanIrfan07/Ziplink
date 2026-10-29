package com.ziplink.redirect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.ziplink.common.exception.GlobalExceptionHandler;
import com.ziplink.common.web.CorrelationIdFilter;
import com.ziplink.config.AsyncConfig;
import com.ziplink.config.ClickEventConfig;
import com.ziplink.config.ClockConfig;
import com.ziplink.config.RedirectCacheConfig;
import com.ziplink.config.ZiplinkProperties;
import com.ziplink.url.RedirectResolver;
import com.ziplink.url.ShortUrlRepository;
import com.ziplink.url.click.ClickRecorder;
import com.ziplink.url.click.ClickStore;
import com.ziplink.url.click.ClientClassifier;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan(basePackageClasses = ZiplinkProperties.class)
@EntityScan(basePackages = "com.ziplink")
@EnableJpaRepositories(basePackageClasses = ShortUrlRepository.class)
@Import({
		AsyncConfig.class,
		ClickEventConfig.class,
		ClockConfig.class,
		RedirectCacheConfig.class,
		RedirectResolver.class,
		ClickRecorder.class,
		ClickStore.class,
		ClientClassifier.class,
		CorrelationIdFilter.class,
		GlobalExceptionHandler.class
})
public class RedirectApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(RedirectApplication.class);
		String port = System.getenv().getOrDefault("REDIRECT_PORT", "8082");
		String[] serviceArgs = new String[args.length + 4];
		serviceArgs[0] = "--server.port=" + port;
		serviceArgs[1] = "--spring.flyway.enabled=false";
		serviceArgs[2] = "--ziplink.click-events.publisher-enabled=true";
		serviceArgs[3] = "--ziplink.click-events.consumer-enabled=false";
		System.arraycopy(args, 0, serviceArgs, 4, args.length);
		application.run(serviceArgs);
	}
}
