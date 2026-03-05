package com.nexus.orderservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.events.model.InventoryResponsePayload;
import com.nexus.orderservice.events.model.OrderEventPayload;
import com.nexus.orderservice.elasticsearch.entity.OrderDocument;
import com.nexus.orderservice.elasticsearch.repository.OrderSearchRepository;
import com.nexus.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Disabled("Tạm thời vô hiệu hóa để tránh lỗi Docker Desktop trên Windows trong quá trình học tập")
public class SagaE2EIntegrationTest extends BaseSagaIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SagaE2EIntegrationTest.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSearchRepository orderSearchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        String esUris = env.getProperty("spring.elasticsearch.uris");
        log.info("🧪 [TEST SETUP] Checking Elasticsearch connection to: {}", esUris);
        
        // Đảm bảo Index tồn tại trước khi chạy test
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(OrderDocument.class);
            if (!indexOps.exists()) {
                log.info("🔍 [TEST] Index 'orders' chưa tồn tại. Đang tạo...");
                indexOps.create();
                indexOps.putMapping();
                log.info("✅ [TEST] Đã tạo Index 'orders' thành công.");
            }
        } catch (Exception e) {
            log.error("❌ [TEST ERROR] Không thể kết nối hoặc khởi tạo Elasticsearch Index: {}", e.getMessage());
            // Không throw exception ở đây để xem các bước verify sau nảy sinh lỗi gì cụ thể hơn
        }
    }

    @Test
    void shouldCompleteSagaSuccessfully_WhenInventoryIsAvailable() throws Exception {
        // 1. Gửi request tạo đơn hàng qua HttpClient
        Map<String, Object> requestBody = Map.of(
                "productId", "PRODUCT-001",
                "quantity", 5);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/v1/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202); // HttpStatus.ACCEPTED
        Map<String, Object> responseMap = objectMapper.readValue(response.body(),
                new TypeReference<Map<String, Object>>() {
                });
        String orderId = (String) responseMap.get("orderId");
        assertThat(orderId).isNotNull();

        // 2. Chờ đợi Saga hoàn tất (Order chuyển sang CONFIRMED)
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        });

        // 3. CQRS: Kiểm tra dữ liệu đã được đồng bộ sang Elasticsearch
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderDocument doc = orderSearchRepository.findById(orderId).orElseThrow();
            assertThat(doc.getStatus()).isEqualTo("CONFIRMED");
        });

        log.info("✅ Saga hoàn tất thành công cho đơn hàng: {}", orderId);
    }

    @Test
    void shouldRollbackSaga_WhenInventoryIsInsufficient() throws Exception {
        // 1. Gửi request với productId đặc biệt "OUT_OF_STOCK"
        Map<String, Object> requestBody = Map.of(
                "productId", "OUT_OF_STOCK",
                "quantity", 999);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/v1/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202);
        Map<String, Object> responseMap = objectMapper.readValue(response.body(),
                new TypeReference<Map<String, Object>>() {
                });
        String orderId = (String) responseMap.get("orderId");
        assertThat(orderId).isNotNull();

        // 2. Chờ đợi Order chuyển sang CANCELLED
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderEntity order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo("CANCELLED");
        });

        // 3. CQRS: Kiểm tra dữ liệu rollback cũng được đồng bộ
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderDocument doc = orderSearchRepository.findById(orderId).orElseThrow();
            assertThat(doc.getStatus()).isEqualTo("CANCELLED");
        });

        log.info("✅ Saga rollback thành công cho đơn hàng: {}", orderId);
    }

    @TestConfiguration
    static class InventoryServiceMockConfig {

        @Autowired
        private KafkaTemplate<byte[], byte[]> kafkaTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @KafkaListener(topics = "order-events-topic", groupId = "inventory-test-group")
        public void handleOrderCreated(byte[] payloadBytes) {
            try {
                OrderEventPayload payload = objectMapper.readValue(payloadBytes, OrderEventPayload.class);
                log.info("🤖 [Mock Inventory] Nhận event OrderCreated cho orderId: {}", payload.getOrderId());

                InventoryResponsePayload response = new InventoryResponsePayload()
                        .withOrderId(payload.getOrderId())
                        .withProductId(payload.getProductId())
                        .withQuantity(payload.getQuantity());

                if ("OUT_OF_STOCK".equals(payload.getProductId())) {
                    response.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED);
                    response.setMessage("Hết hàng trong kho giả lập!");
                } else {
                    response.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_CONFIRMED);
                    response.setMessage("Kho giả lập xác nhận thành công.");
                }

                log.info("🤖 [Mock Inventory] Gửi type: {}", response.getEventType());
                kafkaTemplate.send("inventory-events-topic", payload.getOrderId().getBytes(),
                        objectMapper.writeValueAsBytes(response));
                log.info("🤖 [Mock Inventory] Đã gửi phản hồi {} cho orderId: {}", response.getEventType(),
                        payload.getOrderId());
            } catch (Exception e) {
                log.error("Lỗi khi xử lý message từ order-events-topic: ", e);
            }
        }
    }
}
