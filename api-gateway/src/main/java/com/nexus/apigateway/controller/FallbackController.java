package com.nexus.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý các request dự phòng (Fallback) khi Circuit Breaker
 * (Resilience4j) ngắt mạch.
 * 
 * Khi một Backend Service (như User Service, Product Service) gặp sự cố:
 * - Phản hồi quá chậm (Timeout vượt quá mức cho phép, ví dụ 3s)
 * - Tỷ lệ lỗi (5xx) vượt ngưỡng (ví dụ 50%)
 * Circuit Breaker sẽ chuyển sang trạng thái OPEN (Ngắt mạch) để báo vệ hệ thống
 * khỏi quá tải dây chuyền.
 * Thay vì trả về lỗi 500 ném thẳng vào mặt người dùng, Gateway sẽ route request
 * về các endpoint này
 * để trả về những thông báo lỗi có kiểm soát, thân thiện (Graceful
 * Degradation).
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback Endpoint dành cho User Service.
     * Được kích hoạt khi userServiceCircuitBreaker can thiệp.
     * 
     * @return Mono<ResponseEntity> chứa chuỗi JSON thân thiện thông báo dịch vụ bảo
     *         trì.
     */
    @GetMapping("/userServiceFallback")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        // Mã trạng thái HTTP 503 (Dịch vụ tạm thời không khả dụng - Service
        // Unavailable)
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message",
                "User Service hiện tại đang quá tải hoặc không phản hồi. Vui lòng thử lại sau ít phút.");
        response.put("error", "Circuit Breaker Opened - User Service Fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback Endpoint dành cho Product Service.
     * Được kích hoạt khi productServiceCircuitBreaker can thiệp.
     * 
     * @return Mono<ResponseEntity> chứa chuỗi JSON mô tả trạng thái trễ nải của
     *         Product.
     */
    @GetMapping("/productServiceFallback")
    public Mono<ResponseEntity<Map<String, Object>>> productServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        // Trả về mã lỗi 503 giúp Client (Frontend/Mobile) biết rõ nguyên nhân để hiện
        // popup thử lại, thay vì crash app.
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message",
                "Product Service phản hồi quá chậm. Hệ thống tự động ngắt kết nối để bảo vệ. Vui lòng thử lại sau.");
        response.put("error", "Circuit Breaker Opened - Product Service Timeout/Error");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
