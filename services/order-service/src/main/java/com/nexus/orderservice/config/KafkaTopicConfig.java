package com.nexus.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cấu hình khởi tạo các Topic trên máy chủ Kafka.
 * Topic giống như một "kênh truyền hình". Bên gửi (Order Service) phát sóng vào kênh này,
 * bên nhận (Inventory Service) bật đúng kênh này lên để thu tín hiệu.
 */
@Configuration
public class KafkaTopicConfig {

    // Tên của Topic nơi các sự kiện liên quan đến Đơn hàng (Saga) sẽ được thả vào.
    public static final String SAGA_ORDERS_TOPIC = "saga-orders-topic";

    /**
     * Bean này giúp Spring Boot tự động móc nối lên Kafka Broker và tạo sẵn Topic "saga-orders-topic"
     * nếu nó chưa tồn tại.
     */
    @Bean
    public NewTopic sagaOrdersTopic() {
        return TopicBuilder.name(SAGA_ORDERS_TOPIC)
                // Phân mảnh (Partitions). Càng nhiều partition thì càng xử lý được nhiều luồng // (Concurrency) cùng lúc.
                .partitions(3)
                // Số bản sao dự phòng (Replicas). Vì chạy Local 1 node Kafka nên chỉ để 1.
                .replicas(1)
                .build();
    }
}
