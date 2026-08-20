package com.ethiorental.backend.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated bounded thread pool for async notification dispatch.
 * Kept separate from Spring's default task executor so notification spikes
 * cannot starve business-critical async work elsewhere in the app.
 *
 * Sizing assumptions (flag for review):
 *   core=4, max=10, queue=200 — sized for moderate load.
 *   A notification spike saturating 10 threads + 200-item queue will drop
 *   oldest pending events (DiscardOldestPolicy) rather than blocking callers.
 *   Adjust core/max for your target load; at high volume consider a message
 *   broker (Kafka/RabbitMQ) instead of an in-process thread pool.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
