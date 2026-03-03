package com.nexus.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration().applyPermitDefaultValues();

        corsConfig.addAllowedOrigin("http://localhost:3000");
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setMaxAge(3600L);

        return request -> corsConfig;
    }
}
