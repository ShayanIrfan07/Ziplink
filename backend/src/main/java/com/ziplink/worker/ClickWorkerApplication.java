package com.ziplink.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.ziplink.url.ShortUrlRepository;
import com.ziplink.url.click.ClickEventListener;
import com.ziplink.url.click.ClickStore;
import com.ziplink.url.click.ClientClassifier;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = "com.ziplink")
@EnableJpaRepositories(basePackageClasses = ShortUrlRepository.class)
@Import({ClickStore.class, ClickEventListener.class, ClientClassifier.class})
public class ClickWorkerApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ClickWorkerApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String[] workerArgs = new String[args.length + 4];
		workerArgs[0] = "--spring.main.web-application-type=none";
		workerArgs[1] = "--spring.flyway.enabled=false";
		workerArgs[2] = "--ziplink.click-events.broker-enabled=true";
		workerArgs[3] = "--ziplink.click-events.consumer-enabled=true";
		System.arraycopy(args, 0, workerArgs, 4, args.length);
		application.run(workerArgs);
	}
}
