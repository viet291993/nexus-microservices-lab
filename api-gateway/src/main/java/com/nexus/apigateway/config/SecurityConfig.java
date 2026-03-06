package com.nexus.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> {
                    exchanges
                            .pathMatchers("/eureka/**").permitAll()
                            .pathMatchers("/fallback/**").permitAll()
                            .pathMatchers("/actuator/health", "/actuator/info").permitAll();
                    if (isProd) {
                        exchanges.pathMatchers("/actuator/prometheus").authenticated();
                    } else {
                        exchanges.pathMatchers("/actuator/prometheus").permitAll();
                    }
                    exchanges.anyExchange().authenticated();
                })
                // Kích hoạt cấu hình Gateway trở thành OAuth2 Resource Server
                // Tự động giải mã chuỗi Bearer JWT Token bằng bộ Public key lấy từ Keycloak
                // Issuer URL
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(org.springframework.security.config.Customizer.withDefaults()));

        return http.build();
    }
}
