package com.nexus.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    /**
     * Cấu hình Global CORS WebFilter cho API Gateway.
     * Cấu hình này giúp cho các ứng dụng Frontend (React, Vue, Angular) chạy ở các cổng khác (Vd: localhost:3000)
     * có thể fetch/gọi API tới Gateway (localhost:8080) mà không bị lỗi trình duyệt chặn Cross-Origin.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Cấp phép cho mọi Domain Front-end truy cập.
        // Thực tế ở môi trường Production: Bạn nên sửa "*" thành tên miền thật (Vd: https://nexus-app.com)
        corsConfig.setAllowedOrigins(Arrays.asList("*")); 
        
        // Thời gian (giây) trình duyệt lưu trữ kết quả của Pre-flight request (OPTIONS), giúp giảm tải request thừa
        corsConfig.setMaxAge(3600L); 
        
        // Cấp phép các phương thức HTTP
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Cấp phép các Header (Quan trọng nhất là Authorization chứa JWT và Content-Type)
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

        // Áp dụng bộ quy tắc CORS này cho TẤT CẢ (/**) các Endpoint đi qua Gateway
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
