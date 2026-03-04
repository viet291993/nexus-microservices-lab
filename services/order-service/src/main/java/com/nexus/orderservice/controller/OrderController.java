package com.nexus.orderservice.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.events.model.OrderEventPayload;
import com.nexus.orderservice.repository.OrderRepository;
import com.nexus.orderservice.service.OrderProducerService;

/**
 * REST Controller xử lý các yêu cầu liên quan đến Đơn hàng.
 *
 * Luồng Saga Choreography hoàn chỉnh:
 * 1. Client gửi POST /api/v1/orders → Controller tạo đơn PENDING trong
 * PostgreSQL.
 * 2. Controller đóng gói OrderEventPayload (generated từ AsyncAPI) → gửi vào
 * Kafka topic "order-events-topic".
 * 3. Inventory Service (NestJS) nhận event, trừ kho MongoDB.
 * 4. Inventory gửi phản hồi (CONFIRMED/FAILED) → InventoryResponseConsumer cập
 * nhật DB.
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
     * "productId": "PRODUCT-001",
     * "quantity": 5
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
        OrderEntity order = new OrderEntity(orderId, productId, quantity, "PENDING");
        orderRepository.save(order);
        log.info("💾 [ORDER] Đã lưu đơn hàng {} vào PostgreSQL (status=PENDING)", orderId);

        // Bước 3: Đóng gói và gửi event ORDER_CREATED lên Kafka.
        // Sử dụng OrderEventPayload (generated từ AsyncAPI) với builder pattern và
        // type-safe enums.
        OrderEventPayload event = new OrderEventPayload()
                .withOrderId(orderId)
                .withProductId(productId)
                .withQuantity(quantity)
                .withStatus(OrderEventPayload.OrderStatus.PENDING)
                .withEventType(OrderEventPayload.OrderEventType.ORDER_CREATED);

        producerService.sendOrderEvent(event);
        log.info("📤 [ORDER] Đã gửi event ORDER_CREATED vào Kafka, chờ Inventory xử lý...");

        // Trả ngay cho Client. Không đợi Inventory xử lý xong (Async).
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "orderId", orderId,
                "status", "PENDING",
                "message", "Đơn hàng đã được tiếp nhận và đang chờ xử lý bởi hệ thống kho."));
    }

    /**
     * API lấy danh sách toàn bộ đơn hàng (để kiểm tra trạng thái Saga).
     */
    @GetMapping
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
