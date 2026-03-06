package com.nexus.orderservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class DatabasePasswordValidator {

    private final Environment environment;

    /**
     * Create a DatabasePasswordValidator using the provided Spring environment.
     *
     * @param environment the Spring Environment used to read the `spring.datasource.password` property
     *                    and to determine active profiles (to skip validation in dev/test)
     */
    public DatabasePasswordValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validates that the configured PostgreSQL datasource password is not missing or the default in non-dev/test profiles.
     *
     * If the active profiles do not include "dev" or "test", throws an exception when
     * `spring.datasource.password` is null, blank, or equals `"nexus_password"`.
     *
     * @throws IllegalStateException if validation fails in a non-dev/test profile
     */
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

