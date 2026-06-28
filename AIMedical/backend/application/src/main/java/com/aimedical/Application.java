package com.aimedical;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = "com.aimedical")
@EntityScan("com.aimedical")
@EnableJpaRepositories("com.aimedical")
public class Application {

    private final Environment env;

    public Application(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void validateProfiles() {
        Set<String> activeProfiles = Arrays.stream(env.getActiveProfiles())
                .collect(Collectors.toSet());
        if (activeProfiles.contains("phase0") && !activeProfiles.contains("dev")) {
            throw new IllegalStateException(
                    "phase0 profile 仅允许在 dev 环境使用，请同时激活 dev profile");
        }
    }
}
