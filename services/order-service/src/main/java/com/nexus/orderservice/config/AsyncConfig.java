package com.nexus.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Provide the application's task Executor used for asynchronous task execution.
     *
     * Configures a ThreadPoolTaskExecutor with a core pool size of 2, maximum pool size of 4,
     * queue capacity of 100, and thread name prefix "order-async-".
     *
     * @return an Executor backed by the configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("order-async-");
        executor.initialize();
        return executor;
    }
}
