package com.nexus.orderservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SuppressWarnings("resource")
public abstract class BaseSagaIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BaseSagaIntegrationTest.class);

    @LocalServerPort
    protected int port;

    protected final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    protected static PostgreSQLContainer<?> postgres;
    protected static KafkaContainer kafka;
    protected static MongoDBContainer mongodb;
    protected static ElasticsearchContainer elasticsearch;

    static {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                log.info("🐳 [E2E] Docker detected! Initializing Testcontainers...");
                postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                        .withDatabaseName("nexus_db")
                        .withUsername("nexus_user")
                        .withPassword("nexus_password");

                kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0")
                        .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

                mongodb = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

                elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.2.0")
                        .withEnv("discovery.type", "single-node")
                        .withEnv("xpack.security.enabled", "false")
                        .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

                postgres.start();
                kafka.start();
                mongodb.start();
                elasticsearch.start();
                log.info("🐳 [E2E] Testcontainers started successfully.");
            } else {
                log.warn("⚠️ [E2E] Docker not available. Falling back to host infrastructure.");
            }
        } catch (Exception e) {
            log.error("❌ [E2E] Failed to start Testcontainers. Falling back to host.", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            log.info("🔗 [E2E] Connecting to host PostgreSQL at localhost:5432");
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/nexus_db");
            registry.add("spring.datasource.username", () -> "nexus_user");
            registry.add("spring.datasource.password", () -> "nexus_password");
        }

        if (kafka != null && kafka.isRunning()) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        } else {
            log.info("🔗 [E2E] Connecting to host Kafka at localhost:9092");
            registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        }

        if (mongodb != null && mongodb.isRunning()) {
            registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        } else {
            log.info("🔗 [E2E] Connecting to host MongoDB at localhost:27017");
            registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/nexus_db");
        }

        if (elasticsearch != null && elasticsearch.isRunning()) {
            registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
        } else {
            log.info("🔗 [E2E] Connecting to host Elasticsearch at localhost:9200");
            registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
        }

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "saga-e2e-test-group");

        // Vô hiệu hóa các external services không cần thiết cho Unit Test
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");

        // Đảm bảo Hibernate không bị lỗi Dialect
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
