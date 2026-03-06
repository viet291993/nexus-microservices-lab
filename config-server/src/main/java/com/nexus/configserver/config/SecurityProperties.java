package com.nexus.configserver.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Configuration
@ConfigurationProperties(prefix = "spring.security.user")
@Validated
public class SecurityProperties {

    @EqualsAndHashCode.Include
    @NotBlank(message = "CONFIG_SERVER_PASSWORD must not be blank in production/staging profiles")
    private String password;

    private final Environment environment;

    public SecurityProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (!isDev && (password == null || password.trim().isEmpty() || "dev_password".equals(password))) {
            throw new IllegalStateException(
                    "Security risk: CONFIG_SERVER_PASSWORD is required and cannot be default value in non-dev profiles.");
        }
    }
}
