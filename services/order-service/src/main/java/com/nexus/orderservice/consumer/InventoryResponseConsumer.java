package com.nexus.orderservice.consumer;

import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.events.consumer.IProcessInventoryResponseConsumerService;
import com.nexus.orderservice.events.model.InventoryResponsePayload;
import com.nexus.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Kafka Consumer lắng nghe phản hồi từ Inventory Service.
 *
 * Lưu ý: Class này THỰC THI (implements) interface do ZenWave sinh tự động:
 * IProcessInventoryResponseConsumerService.
 * Nó KHÔNG cần @KafkaListener vì việc lấy message từ Topic (Routing/Deserialize)
 * do Spring Cloud Stream và ZenWave lo liệu đằng sau.
 */
@Service
public class InventoryResponseConsumer implements IProcessInventoryResponseConsumerService {

    private static final Logger log = LoggerFactory.getLogger(InventoryResponseConsumer.class);

    private final OrderRepository orderRepository;

    public InventoryResponseConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Hàm này được ZenWave Generated Controller gọi mỗi khi có message mới.
     * Cực kỳ mạnh vì nó là Interface chuẩn (Type-safe).
     */
    @Override
    public void processInventoryResponse(InventoryResponsePayload payload, InventoryResponsePayloadHeaders headers) {
        InventoryResponsePayload.InventoryEventType eventType = payload.getEventType();
        String orderId = payload.getOrderId();
        String message = payload.getMessage();

        log.info("📩 [CONSUMER via ZenWave] Nhận phản hồi từ Kho: orderId={}, eventType={}, message={}",
                orderId, eventType, message);

        // Tìm đơn hàng trong PostgreSQL.
        Optional<OrderEntity> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.error("❌ [CONSUMER] Không tìm thấy đơn hàng {} trong Database để cập nhật!", orderId);
            return;
        }

        OrderEntity order = optionalOrder.get();

        // Xử lý Saga
        if (InventoryResponsePayload.InventoryEventType.INVENTORY_CONFIRMED == eventType) {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            log.info("✅ [SAGA CONFIRMED] Đơn hàng {} → CONFIRMED. Kho đã trừ.", orderId);

        } else if (InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED == eventType) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.warn("🚫 [SAGA ROLLBACK] Đơn hàng {} → CANCELLED. Lý do: {}", orderId, message);
        }
    }
}
