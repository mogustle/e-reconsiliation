package com.toulios.reconsiliation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configures asynchronous processing for the application.
 *
 * <p>This configuration provides multiple executor options for CSV processing:
 * a traditional {@link ThreadPoolTaskExecutor} and a virtual threads executor.
 * Virtual threads (Java 21+) are ideal for I/O-bound CSV operations as they
 * provide better resource utilization and higher concurrency with lower memory overhead.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Virtual threads executor for CSV parsing tasks via {@code @Async("csvExecutor")}.
	 *
	 * @return a virtual thread executor that creates a new virtual thread per task
	 */
	@Bean(name = "csvExecutor")
	public Executor csvExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}


