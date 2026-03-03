package com.nexus.orderservice.consumer;

import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Kafka Consumer lắng nghe phản hồi từ Inventory Service.
 *
 * Đây là nửa SAU của vòng lặp Saga Choreography:
 *   - Inventory đã xử lý xong (trừ kho OK hoặc thất bại).
 *   - Inventory gửi kết quả vào topic "saga-inventory-response".
 *   - Consumer này nhận kết quả đó và CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG trong PostgreSQL.
 *
 * Kết quả có thể là:
 *   - "INVENTORY_CONFIRMED" → Đơn hàng chuyển từ PENDING sang CONFIRMED.
 *   - "INVENTORY_FAILED"    → Đơn hàng chuyển từ PENDING sang CANCELLED (Saga Rollback).
 */
@Component
public class InventoryResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryResponseConsumer.class);

    private final OrderRepository orderRepository;

    public InventoryResponseConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Handler tự động được Spring Kafka gọi mỗi khi có message mới
     * trên topic "saga-inventory-response".
     *
     * @param response Map chứa dữ liệu phản hồi từ Inventory (JSON đã được deserialize).
     */
    @KafkaListener(
            topics = "saga-inventory-response",
            groupId = "order-service-group"
    )
    public void handleInventoryResponse(Map<String, Object> response) {
        String eventType = (String) response.get("eventType");
        String orderId = (String) response.get("orderId");
        String message = (String) response.get("message");

        log.info("📩 [CONSUMER] Nhận phản hồi từ Inventory: orderId={}, eventType={}, message={}",
                orderId, eventType, message);

        // Tìm đơn hàng trong PostgreSQL.
        Optional<OrderEntity> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.error("❌ [CONSUMER] Không tìm thấy đơn hàng {} trong Database để cập nhật!", orderId);
            return;
        }

        OrderEntity order = optionalOrder.get();

        // Xử lý Saga: Cập nhật trạng thái đơn hàng dựa trên phản hồi của Inventory.
        if ("INVENTORY_CONFIRMED".equals(eventType)) {
            // Trừ kho thành công → Đơn hàng được XÁC NHẬN.
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            log.info("✅ [SAGA CONFIRMED] Đơn hàng {} → CONFIRMED. Kho đã trừ thành công.", orderId);

        } else if ("INVENTORY_FAILED".equals(eventType)) {
            // Trừ kho thất bại → ROLLBACK: Hủy đơn hàng (Compensating Transaction).
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.warn("🚫 [SAGA ROLLBACK] Đơn hàng {} → CANCELLED. Lý do: {}", orderId, message);
        }
    }
}
