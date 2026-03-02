package com.nexus.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Xác định "Định danh" (Key) của mỗi Client truy cập vào hệ thống để đếm số lượng Request.
     * Cấu hình dưới đây sẽ đếm số Lượt truy cập dựa theo IP của người dùng.
     * Nghĩa là mỗi địa chỉ IP chỉ được cấp phát một số lượng Token Request nhất định trong 1 giây.
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress()
        );
    }
}
