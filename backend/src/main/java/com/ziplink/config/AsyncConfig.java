package com.ziplink.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean(name = "clickExecutor")
	Executor clickExecutor(ZiplinkProperties properties) {
		if (!properties.clicksAsync()) {
			return new SyncTaskExecutor();
		}
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("click-");
		executor.setRejectedExecutionHandler((runnable, pool) -> {
			log.warn("Click recording queue is full; dropping a click event");
			new ThreadPoolExecutor.DiscardPolicy().rejectedExecution(runnable, pool);
		});
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (exception, method, params) ->
				log.warn("Click recording failed: {}", exception.getMessage());
	}
}
