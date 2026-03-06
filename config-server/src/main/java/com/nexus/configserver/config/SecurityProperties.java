package com.nexus.configserver.config;

import jakarta.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Configuration
@ConfigurationProperties(prefix = "spring.security.user")
public class SecurityProperties {

    @EqualsAndHashCode.Include
    private String password;

    private final Environment environment;

    public SecurityProperties(Environment environment) {
        this.environment = environment;
    }

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
