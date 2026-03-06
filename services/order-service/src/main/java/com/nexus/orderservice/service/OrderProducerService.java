package com.nexus.orderservice.service;

import com.nexus.orderservice.events.model.OrderEventPayload;
import com.nexus.orderservice.events.producer.DefaultServiceEventsProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service chịu trách nhiệm "bắn" (Produce) các sự kiện đơn hàng vào Kafka
 * Topic.
 *
 * Trong mô hình Saga Choreography:
 * - Order Service KHÔNG gọi trực tiếp API của Inventory Service.
 * - Nó dùng DefaultServiceEventsProducer (ZenWave sinh tự động từ AsyncAPI).
 * - Logic Pub/Sub bằng Spring Cloud Stream đã được cấu hình trong
 * application.yml.
 */
@Service
public class OrderProducerService {

    private static final Logger log = LoggerFactory.getLogger(OrderProducerService.class);

    // Dùng Interface Producer tự động sinh thay vì viết tay KafkaTemplate
    private final DefaultServiceEventsProducer serviceEventsProducer;

    public OrderProducerService(DefaultServiceEventsProducer serviceEventsProducer) {
        this.serviceEventsProducer = serviceEventsProducer;
    }

    /**
     * Send an order event to the Kafka topic "order-events-topic".
     *
     * Sets the `kafka_messageKey` header to the event's orderId bytes to ensure consistent Kafka partitioning.
     *
     * @param event the OrderEventPayload containing at minimum a non-blank `orderId` and an `eventType`
     * @throws IllegalArgumentException if `event` is null or `event.getOrderId()` is null or blank
     * @throws IllegalStateException if the underlying producer reports failure to dispatch the event
     */
    public void sendOrderEvent(OrderEventPayload event) {
        if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
            throw new IllegalArgumentException("OrderEventPayload and orderId are required");
        }
        log.info("📦 [PRODUCER] Dispatching OrderEventPayload to Kafka via ZenWave Producer: orderId={}, eventType={}",
                event.getOrderId(), event.getEventType());

        // ZenWave creates sendOrderEvents handles headers (like messageKey).
        // Using orderId as message key for consistent partitioning in Kafka.
        var headers = new DefaultServiceEventsProducer.OrderEventPayloadHeaders();
        headers.put("kafka_messageKey", event.getOrderId().getBytes());

        boolean success = serviceEventsProducer.sendOrderEvents(event, headers);

        if (success) {
            log.info("✅ [PRODUCER] Successfully dispatched event for order: {}", event.getOrderId());
        } else {
            String message = String.format(
                    "Failed to dispatch event for order: %s. Producer returned false.",
                    event.getOrderId());
            log.error("❌ [PRODUCER] {}", message);
            throw new IllegalStateException(message);
        }
    }
}
