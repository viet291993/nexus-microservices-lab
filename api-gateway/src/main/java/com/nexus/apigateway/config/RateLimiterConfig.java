package com.nexus.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import java.net.InetAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * Xác định "Định danh" (Key) của mỗi Client truy cập vào hệ thống để đếm số
     * lượng Request.
     * Cấu hình dưới đây sẽ đếm số Lượt truy cập dựa theo IP của người dùng.
     * Nghĩa là mỗi địa chỉ IP chỉ được cấp phát một số lượng Token Request nhất
     * định trong 1 giây.
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            String clientIp;
            if (remoteAddress != null) {
                InetAddress address = remoteAddress.getAddress(); // Extracted local variable
                if (address != null) {
                    clientIp = address.getHostAddress();
                } else {
                    clientIp = "127.0.0.1"; // Fallback IP dành cho môi trường Test (WebTestClient)
                }
            } else {
                clientIp = "127.0.0.1"; // Fallback IP dành cho môi trường Test (WebTestClient)
            }
            return Mono.just(clientIp);
        };
    }
}
