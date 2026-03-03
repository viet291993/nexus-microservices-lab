package com.nexus.orderservice.controller;

import com.nexus.orderservice.dto.OrderEvent;
import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.repository.OrderRepository;
import com.nexus.orderservice.service.OrderProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller xử lý các yêu cầu liên quan đến Đơn hàng.
 *
 * Luồng Saga Choreography hoàn chỉnh (Bước 4):
 *   1. Client gửi POST /api/v1/orders → Controller tạo đơn PENDING trong PostgreSQL.
 *   2. Controller đóng gói OrderEvent → gửi vào Kafka topic "saga-orders-topic".
 *   3. Inventory Service (NestJS) nhận event, trừ kho MongoDB.
 *   4. Inventory gửi phản hồi (CONFIRMED/FAILED) → InventoryResponseConsumer cập nhật DB.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderProducerService producerService;
    private final OrderRepository orderRepository;

    public OrderController(OrderProducerService producerService, OrderRepository orderRepository) {
        this.producerService = producerService;
        this.orderRepository = orderRepository;
    }

    /**
     * API tạo đơn hàng mới.
     *
     * Request Body mẫu (JSON):
     * {
     *   "productId": "PRODUCT-001",
     *   "quantity": 5
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody Map<String, Object> request) {
        // Bước 1: Sinh UUID và trích xuất dữ liệu từ request body.
        String orderId = UUID.randomUUID().toString();
        String productId = (String) request.get("productId");
        int quantity = (int) request.get("quantity");

        log.info("🛒 [ORDER] Nhận yêu cầu tạo đơn hàng: productId={}, quantity={}", productId, quantity);

        // Bước 2: Lưu đơn hàng vào PostgreSQL với trạng thái PENDING.
        // Đơn sẽ nằm ở trạng thái này cho đến khi Inventory phản hồi qua Kafka.
        OrderEntity order = new OrderEntity(orderId, productId, quantity, "PENDING");
        orderRepository.save(order);
        log.info("💾 [ORDER] Đã lưu đơn hàng {} vào PostgreSQL (status=PENDING)", orderId);

        // Bước 3: Đóng gói và gửi event ORDER_CREATED lên Kafka.
        OrderEvent event = new OrderEvent(orderId, productId, quantity, "PENDING", "ORDER_CREATED");
        producerService.sendOrderEvent(event);
        log.info("📤 [ORDER] Đã gửi event ORDER_CREATED vào Kafka, chờ Inventory xử lý...");

        // Trả ngay cho Client. Không đợi Inventory xử lý xong (Async).
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "orderId", orderId,
                "status", "PENDING",
                "message", "Đơn hàng đã được tiếp nhận và đang chờ xử lý bởi hệ thống kho."
        ));
    }

    /**
     * API lấy danh sách toàn bộ đơn hàng (để kiểm tra trạng thái Saga).
     */
    @GetMapping
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
