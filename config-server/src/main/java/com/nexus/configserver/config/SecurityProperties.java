package com.nexus.configserver.config;

import jakarta.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Getter
@Setter
@EqualsAndHashCode(exclude = "password")
@ConfigurationProperties(prefix = "spring.security.user")
public class SecurityProperties {

    private String password;

    private final Environment environment;

    /**
     * Creates a SecurityProperties instance backed by the given Spring Environment.
     *
     * @param environment the Spring Environment used to inspect active profiles for runtime validation
     */
    public SecurityProperties(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validates the configured security password for non-dev Spring profiles.
     *
     * <p>Validation is skipped when the active profiles include "dev". For non-dev profiles, the
     * property bound to {@code spring.security.user.password} must be present, not blank, and must
     * not equal the default value {@code "dev_password"}.
     *
     * @throws IllegalStateException if the password is null in a non-dev profile
     * @throws IllegalStateException if the password is blank (only whitespace) in a non-dev profile
     * @throws IllegalStateException if the password equals the default value {@code "dev_password"} in a non-dev profile
     */
    @PostConstruct
    public void validate() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (isDev) {
            return;
        }
        if (password == null) {
            throw new IllegalStateException(
                    "CONFIG_SERVER_PASSWORD is required in non-dev profiles but was not set.");
        }
        if (password.trim().isEmpty()) {
            throw new IllegalStateException(
                    "CONFIG_SERVER_PASSWORD must not be blank in non-dev profiles.");
        }
        if ("dev_password".equals(password)) {
            throw new IllegalStateException(
                    "Security risk: CONFIG_SERVER_PASSWORD cannot be the default 'dev_password' in non-dev profiles.");
        }
    }
}
