package com.aimedical.modules.prescription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class PrescriptionThreadPoolConfig {

    private static final int MAX_AI_TASK_POOL_SIZE = 50;

    @Bean("aiTaskExecutor")
    public ExecutorService aiTaskExecutor() {
        AtomicInteger counter = new AtomicInteger(0);
        return new ThreadPoolExecutor(
            0, MAX_AI_TASK_POOL_SIZE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("ai-task-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        );
    }
}
