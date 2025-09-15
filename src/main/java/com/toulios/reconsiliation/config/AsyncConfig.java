package com.toulios.reconsiliation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures asynchronous processing for the application.
 *
 * <p>This configuration declares a dedicated {@link java.util.concurrent.Executor}
 * named {@code csvExecutor} backed by a {@link ThreadPoolTaskExecutor}. It is
 * tuned for CPU-bound CSV parsing by sizing the core pool to the number of
 * available processors and a reasonable queue capacity.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Executor used for CSV parsing tasks via {@code @Async("csvExecutor")}.
	 *
	 * @return a configured thread pool executor
	 */
	@Bean(name = "csvExecutor")
	public Executor csvExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
		executor.setMaxPoolSize(Math.max(Runtime.getRuntime().availableProcessors(), 4));
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("csv-processor-");
		executor.initialize();
		return executor;
	}
}


