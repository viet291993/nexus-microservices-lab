package com.nexus.apigateway;

import com.nexus.apigateway.config.CorsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CORS Configuration Tests")
@SuppressWarnings("null")
class CorsConfigTest {

    @Test
    @DisplayName("Kiểm tra bộ quy tắc CORS tạo ra đúng cấu hình mong muốn")
    void testCorsConfigurationRules() {
        CorsConfig corsConfigSetup = new CorsConfig();
        CorsConfigurationSource source = corsConfigSetup.corsConfigurationSource();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users")
                .header("Origin", "http://localhost:3000")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        CorsConfiguration config = source.getCorsConfiguration(exchange);

        assertNotNull(config, "CorsConfiguration không được null khi có request tới URI hợp lệ");

        // Kiểm tra Origin
        assertTrue(
                (config.getAllowedOrigins() != null && config.getAllowedOrigins().contains("http://localhost:3000")) ||
                        (config.getAllowedOriginPatterns() != null && config.getAllowedOriginPatterns().contains("*")),
                "Phải cho phép Origin localhost:3000 hoặc wildcard *");

        // Kiểm tra Method
        assertTrue(
                config.getAllowedMethods() != null
                        && (config.getAllowedMethods().contains("*") || config.getAllowedMethods().contains("GET")),
                "Phải cho phép phương thức GET hoặc wildcard *");

        // Kiểm tra MaxAge
        assertEquals(3600L, config.getMaxAge(), "Max age thiết lập cache Preflight cần là 3600 giây");
    }
}
