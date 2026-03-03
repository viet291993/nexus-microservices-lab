package com.nexus.orderservice.controller;

import com.nexus.orderservice.dto.OrderEvent;
import com.nexus.orderservice.service.OrderProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller xử lý các yêu cầu liên quan đến Đơn hàng.
 *
 * Luồng xử lý tổng quan (Saga Choreography):
 *   1. Client gửi POST /api/v1/orders với body chứa productId và quantity.
 *   2. Controller tạo OrderEvent với trạng thái PENDING rồi "quăng" vào Kafka.
 *   3. Inventory Service (NestJS) đang lắng nghe Kafka, nhận event và xử lý trừ kho.
 *   4. Nếu trừ kho thành công => Inventory gửi event INVENTORY_CONFIRMED.
 *      Nếu trừ kho thất bại  => Inventory gửi event INVENTORY_FAILED.
 *   5. Order Service nhận lại event phản hồi và cập nhật trạng thái đơn (CONFIRMED / CANCELLED).
 *
 * Giai đoạn hiện tại (Bước 3): Chỉ tập trung vào bước 1-3 (Tạo đơn -> Gửi Kafka -> Inventory nhận).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderProducerService producerService;

    public OrderController(OrderProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * API tạo đơn hàng mới.
     *
     * Request Body mẫu (JSON):
     * {
     *   "productId": "PRODUCT-001",
     *   "quantity": 5
     * }
     *
     * Response: Trả về orderId (UUID) mới được tạo.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody Map<String, Object> request) {
        // Bước 1: Sinh mã đơn hàng ngẫu nhiên (UUID) đảm bảo tính duy nhất toàn cầu.
        String orderId = UUID.randomUUID().toString();
        String productId = (String) request.get("productId");
        int quantity = (int) request.get("quantity");

        log.info("🛒 [ORDER] Nhận yêu cầu tạo đơn hàng: productId={}, quantity={}", productId, quantity);

        // Bước 2: Đóng gói thông tin đơn hàng thành một OrderEvent.
        //   - status = "PENDING": Đơn đang chờ xử lý (chưa biết kho có đủ hàng không).
        //   - eventType = "ORDER_CREATED": Đánh dấu đây là sự kiện MỚI TẠO (Saga bắt đầu).
        OrderEvent event = new OrderEvent(orderId, productId, quantity, "PENDING", "ORDER_CREATED");

        // Bước 3: Ném event vào Kafka Topic. Inventory Service sẽ nhận được ở phía bên kia.
        producerService.sendOrderEvent(event);

        log.info("📤 [ORDER] Đã gửi đơn hàng {} vào Kafka, đang chờ Inventory xử lý...", orderId);

        // Trả ngay kết quả cho Client. Không đợi Inventory xử lý xong (Asynchronous).
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "orderId", orderId,
                "status", "PENDING",
                "message", "Đơn hàng đã được tiếp nhận và đang chờ xử lý bởi hệ thống kho."
        ));
    }
}
