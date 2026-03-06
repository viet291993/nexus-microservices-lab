package com.nexus.orderservice.consumer;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.context.ApplicationEventPublisher;

import org.springframework.transaction.annotation.Transactional;
import com.nexus.orderservice.elasticsearch.events.OrderSyncEvent;
import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.entity.OrderStatus;
import com.nexus.orderservice.events.consumer.IProcessInventoryResponseConsumerService;
import com.nexus.orderservice.events.model.InventoryResponsePayload;
import com.nexus.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

/**
 * Kafka Consumer lắng nghe phản hồi từ Inventory Service.
 *
 * Lưu ý: Class này THỰC THI (implements) interface do ZenWave sinh tự động:
 * IProcessInventoryResponseConsumerService.
 * Nó KHÔNG cần @KafkaListener vì việc lấy message từ Topic
 * (Routing/Deserialize)
 * do Spring Cloud Stream và ZenWave lo liệu đằng sau.
 */
@Service
public class InventoryResponseConsumer implements IProcessInventoryResponseConsumerService {

        private static final Logger log = LoggerFactory.getLogger(InventoryResponseConsumer.class);

        private final OrderRepository orderRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final MeterRegistry meterRegistry;
        private final Counter orderConfirmedCounter;
        private final Counter orderCancelledCounter;

        /**
         * Create a new InventoryResponseConsumer and initialize its metrics.
         *
         * @param orderRepository repository for loading and persisting orders
         * @param eventPublisher  application event publisher used to emit
         *                        OrderSyncEvent events
         * @param meterRegistry   MeterRegistry used to register Micrometer counters for
         *                        confirmed and cancelled orders
         */
        public InventoryResponseConsumer(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher,
                        MeterRegistry meterRegistry) {
                this.orderRepository = orderRepository;
                this.eventPublisher = eventPublisher;
                this.meterRegistry = meterRegistry;
                this.orderConfirmedCounter = Counter.builder("order_confirmed_total")
                                .description("Total number of orders successfully confirmed by inventory")
                                .register(meterRegistry);
                this.orderCancelledCounter = Counter.builder("order_cancelled_total")
                                .description("Total number of orders cancelled due to inventory failure")
                                .register(meterRegistry);
        }

        /**
         * Handle inventory service responses to update order status, record metrics,
         * and publish order sync events.
         *
         * Processes an InventoryResponsePayload and, based on its event type, updates
         * the corresponding order's
         * status to CONFIRMED or CANCELLED, increments the matching Micrometer counter,
         * and publishes an OrderSyncEvent.
         * The method performs idempotent checks: it skips processing when the order is
         * already in the target status.
         *
         * @param payload the inventory response containing the event type, order ID,
         *                and optional message
         * @param headers transport-level headers supplied by the consumer; provided by
         *                the generated controller
         */
        @Override
        @Transactional
        public void processInventoryResponse(InventoryResponsePayload payload,
                        InventoryResponsePayloadHeaders headers) {
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
                        if (OrderStatus.CONFIRMED == order.getStatus()) {
                                log.warn("♻️ [SAGA CONFIRMED] Bỏ qua Inventory CONFIRMED lặp lại cho đơn hàng {} (đã CONFIRMED trước đó).",
                                                orderId);
                                return;
                        }
                        order.setStatus(OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                        orderConfirmedCounter.increment();
                        log.info("✅ [SAGA CONFIRMED] Đơn hàng {} → CONFIRMED. Kho đã trừ.", orderId);

                        eventPublisher.publishEvent(
                                        new OrderSyncEvent(this, orderId, order.getProductId(), order.getQuantity(),
                                                        OrderStatus.CONFIRMED));

                } else if (InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED == eventType) {
                        // Idempotent: nếu đơn hàng đã bị CANCELLED trước đó thì bỏ qua failure event
                        // lặp lại.
                        if (OrderStatus.CANCELLED == order.getStatus()) {
                                log.warn("♻️ [SAGA ROLLBACK] Bỏ qua Inventory FAILED lặp lại cho đơn hàng {} (đã CANCELLED trước đó).",
                                                orderId);
                                return;
                        }
                        order.setStatus(OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        orderCancelledCounter.increment();
                        log.warn("🚫 [SAGA ROLLBACK] Đơn hàng {} → CANCELLED. Lý do: {}", orderId, message);

                        eventPublisher.publishEvent(
                                        new OrderSyncEvent(this, orderId, order.getProductId(), order.getQuantity(),
                                                        OrderStatus.CANCELLED));
                } else {
                        Counter unknownEventCounter = Counter.builder("inventory_unknown_event_type_total")
                                        .description("Total number of inventory responses with unknown eventType")
                                        .tag("eventType", eventType != null ? eventType.name() : "null")
                                        .register(meterRegistry);
                        unknownEventCounter.increment();
                        log.warn("❓ [CONSUMER] Nhận được InventoryEventType không xác định: {}", eventType);
                }
        }
}
