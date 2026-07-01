package com.aimedical.modules.medicalrecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MedicalRecordThreadPoolConfig {

    @Bean("medicalRecordExecutor")
    public ExecutorService medicalRecordExecutor() {
        return Executors.newCachedThreadPool();
    }
}
