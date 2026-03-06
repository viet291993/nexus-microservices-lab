package com.nexus.orderservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class DatabasePasswordValidator {

    private final Environment environment;

    public DatabasePasswordValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validatePostgresPassword() {
        boolean isDevProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));

        String rawPassword = environment.getProperty("spring.datasource.password");

        if (!isDevProfile && (rawPassword == null || rawPassword.isBlank() || "nexus_password".equals(rawPassword))) {
            throw new IllegalStateException("Security risk: spring.datasource.password must be set to a non-default value in non-dev profiles.");
        }
    }
}

