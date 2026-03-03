package com.nexus.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/eureka/**").permitAll() // Cho phép truy cập Eureka dashboard (nếu cần)
                        .pathMatchers("/fallback/**").permitAll() // Cho phép truy cập các route Fallback
                                                                  // (CircuitBreaker)
                        .pathMatchers("/actuator/**").permitAll() // Mở Actuator để Monitor sức khoẻ System
                        .anyExchange().authenticated() // Mọi request khác nhắm vào backend đều phải có lệnh Token JWT
                )
                // Kích hoạt cấu hình Gateway trở thành OAuth2 Resource Server
                // Tự động giải mã chuỗi Bearer JWT Token bằng bộ Public key lấy từ Keycloak
                // Issuer URL
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(org.springframework.security.config.Customizer.withDefaults()));

        return http.build();
    }
}
