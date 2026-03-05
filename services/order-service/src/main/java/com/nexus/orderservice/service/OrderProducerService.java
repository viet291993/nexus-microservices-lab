package com.nexus.orderservice.service;

import com.nexus.orderservice.events.model.OrderEventPayload;
import com.nexus.orderservice.events.producer.DefaultServiceEventsProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service chịu trách nhiệm "bắn" (Produce) các sự kiện đơn hàng vào Kafka Topic.
 *
 * Trong mô hình Saga Choreography:
 *   - Order Service KHÔNG gọi trực tiếp API của Inventory Service.
 *   - Nó dùng DefaultServiceEventsProducer (ZenWave sinh tự động từ AsyncAPI).
 *   - Logic Pub/Sub bằng Spring Cloud Stream đã được cấu hình trong application.yml.
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
     * Gửi một sự kiện đơn hàng lên Kafka Topic "order-events-topic".
     *
     * @param event Đối tượng OrderEventPayload (generated từ AsyncAPI)
     */
    public void sendOrderEvent(OrderEventPayload event) {
        if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
            throw new IllegalArgumentException("event và event.orderId là bắt buộc");
        }
        log.info("📦 [PRODUCER] Đang gửi OrderEventPayload lên Kafka qua ZenWave Generated Producer: {}", event);

        // ZenWave tạo hàm sendOrderEvents hỗ trợ thêm headers (như messageKey).
        // Phải đưa orderId vào Headers để Kafka định tuyến đúng Partition.
        var headers = new DefaultServiceEventsProducer.OrderEventPayloadHeaders();
        headers.put("kafka_messageKey", event.getOrderId().getBytes());

        boolean success = serviceEventsProducer.sendOrderEvents(event, headers);

        if (success) {
            log.info("✅ [PRODUCER] Gửi thành công event {} thông qua Spring Cloud Stream!", event.getOrderId());
        } else {
            log.error("❌ [PRODUCER] Gửi thất bại cho OrderEventPayload: {}", event.getOrderId());
        }
    }
}
