package com.nexus.orderservice.consumer;

import com.nexus.orderservice.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka Consumer lắng nghe phản hồi từ Inventory Service.
 *
 * Trong luồng Saga Choreography:
 *   - Sau khi Order gửi event ORDER_CREATED, Inventory xử lý trừ kho.
 *   - Inventory gửi phản hồi vào topic "saga-inventory-response" (sẽ tạo sau).
 *   - Consumer này tiếp nhận kết quả phản hồi đó.
 *   - Nếu eventType == "INVENTORY_CONFIRMED" => Cập nhật đơn hàng thành CONFIRMED.
 *   - Nếu eventType == "INVENTORY_FAILED"    => Rollback đơn hàng thành CANCELLED.
 *
 * Giai đoạn hiện tại (Bước 3): Chỉ log ra console để kiểm chứng luồng giao tiếp.
 */
@Component
public class InventoryResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryResponseConsumer.class);

    /**
     * Phương thức này sẽ tự động được Spring Kafka gọi mỗi khi có message mới
     * xuất hiện trên topic "saga-inventory-response".
     *
     * @param response Map chứa dữ liệu phản hồi từ Inventory Service (JSON đã được deserialize).
     */
    @KafkaListener(
            topics = "saga-inventory-response",
            groupId = "order-service-group"
    )
    public void handleInventoryResponse(Map<String, Object> response) {
        String eventType = (String) response.get("eventType");
        String orderId = (String) response.get("orderId");

        log.info("📩 [CONSUMER] Nhận phản hồi từ Inventory: orderId={}, eventType={}", orderId, eventType);

        // Logic xử lý Saga Rollback / Confirm sẽ được bổ sung chi tiết ở Bước 4.
        if ("INVENTORY_CONFIRMED".equals(eventType)) {
            log.info("✅ [SAGA] Đơn hàng {} đã được XÁC NHẬN bởi Inventory. Cập nhật trạng thái -> CONFIRMED", orderId);
            // TODO: Cập nhật trạng thái đơn hàng trong PostgreSQL thành CONFIRMED.
        } else if ("INVENTORY_FAILED".equals(eventType)) {
            log.warn("🚫 [SAGA] Đơn hàng {} BỊ TỪ CHỐI bởi Inventory (Hết hàng). Rollback trạng thái -> CANCELLED", orderId);
            // TODO: Cập nhật trạng thái đơn hàng trong PostgreSQL thành CANCELLED (Bù trừ / Compensating).
        }
    }
}
