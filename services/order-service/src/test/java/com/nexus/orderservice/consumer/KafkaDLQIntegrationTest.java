package com.nexus.orderservice.consumer;

import com.nexus.orderservice.BaseSagaIntegrationTest;
import com.nexus.orderservice.events.model.InventoryResponsePayload;
import com.nexus.orderservice.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@DirtiesContext
public class KafkaDLQIntegrationTest extends BaseSagaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @Test
    public void testDLQTriggeringOnConsumerFailure() {
        // 1. Giả lập lỗi khi tìm kiếm đơn hàng để kích hoạt Retry -> DLQ
        Mockito.doThrow(new RuntimeException("Simulated Database Failure for DLQ Test"))
                .when(orderRepository).findById(anyString());

        // 2. Gửi một message tới topic chính
        String orderId = "test-order-dlq-" + java.util.UUID.randomUUID();
        String groupId = "test-dlq-group-" + java.util.UUID.randomUUID();

        InventoryResponsePayload payload = new InventoryResponsePayload()
                .withOrderId(orderId)
                .withEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_CONFIRMED)
                .withMessage("Test DLQ");

        kafkaTemplate.send("inventory-events-topic", payload);

        // 3. Thiết lập consumer để lắng nghe từ DLQ topic
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), groupId, false);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singleton("inventory-events-topic.dlq"));

        // 4. Đợi và kiểm tra xem message có xuất hiện trong DLQ không (sau 3 lần retry)
        ConsumerRecord<String, String> received = KafkaTestUtils.getSingleRecord(consumer, "inventory-events-topic.dlq", Duration.ofSeconds(20));

        assertThat(received).isNotNull();
        assertThat(received.value()).contains(orderId);
        
        consumer.close();
    }
}
