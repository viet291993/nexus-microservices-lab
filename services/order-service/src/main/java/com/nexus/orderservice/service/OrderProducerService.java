package com.nexus.orderservice.service;

import com.nexus.orderservice.config.KafkaTopicConfig;
import com.nexus.orderservice.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service chịu trách nhiệm "bắn" (Produce) các sự kiện đơn hàng vào Kafka Topic.
 *
 * Trong mô hình Saga Choreography:
 *   - Order Service KHÔNG gọi trực tiếp API của Inventory Service (không có HTTP call chéo).
 *   - Thay vào đó, nó "quăng" một bưu kiện (OrderEvent) vào kênh truyền thông Kafka.
 *   - Ai cần thì tự đến kênh đó mà "nhặt" bưu kiện lên xử lý (Inventory, Notification...).
 *   => Đây chính là kiến trúc Event-Driven, giúp các Service không phụ thuộc trực tiếp vào nhau.
 */
@Service
public class OrderProducerService {

    private static final Logger log = LoggerFactory.getLogger(OrderProducerService.class);

    // KafkaTemplate là công cụ chính mà Spring cung cấp để gửi Message lên Kafka.
    // <String, OrderEvent>: Key là String (orderId), Value là đối tượng OrderEvent (tự động chuyển sang JSON).
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducerService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Gửi một sự kiện đơn hàng lên Kafka Topic "saga-orders-topic".
     *
     * Giải thích tham số send():
     *   - Tham số 1 (Topic Name): Tên kênh Kafka mà message sẽ được ném vào.
     *   - Tham số 2 (Key): Mã đơn hàng. Kafka dùng Key để phân phối message vào đúng Partition.
     *     Các message cùng Key sẽ luôn vào cùng 1 Partition => đảm bảo thứ tự xử lý cho cùng 1 đơn hàng.
     *   - Tham số 3 (Value): Nội dung bưu kiện (OrderEvent) sẽ được tự động serialize sang JSON.
     *
     * @param event Đối tượng OrderEvent chứa thông tin đơn hàng cần gửi.
     */
    public void sendOrderEvent(OrderEvent event) {
        log.info("📦 [PRODUCER] Đang gửi OrderEvent lên Kafka Topic '{}': {}", KafkaTopicConfig.SAGA_ORDERS_TOPIC, event);

        // Gửi bất đồng bộ (Async). CompletableFuture cho phép ta xử lý kết quả mà không block luồng chính.
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(KafkaTopicConfig.SAGA_ORDERS_TOPIC, event.orderId(), event);

        // Callback: Xử lý khi gửi thành công hoặc thất bại.
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Gửi thành công: Log ra thông tin partition và offset nơi message được lưu trữ.
                log.info("✅ [PRODUCER] Gửi thành công! Topic={}, Partition={}, Offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Gửi thất bại: Log lỗi để debug.
                log.error("❌ [PRODUCER] Gửi thất bại cho OrderEvent: {}", event, ex);
            }
        });
    }
}
