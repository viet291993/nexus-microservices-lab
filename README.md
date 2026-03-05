# Nexus: Microservices Exploration Lab

*A modern playground for decoding distributed systems and backend architectures.*

---

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/viet291993/nexus-microservices-lab?utm_source=oss&utm_medium=github&utm_campaign=viet291993%2Fnexus-microservices-lab&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

## 🏗️ Architecture

* **API Gateway:** Spring Cloud Gateway (Rate limiting, Dynamic Routing).
* **Auth:** Stateless JWT & OAuth2.
* **Communication:**
    * **Sync:** REST/gRPC for consistency.
    * **Async:** Event-driven via RabbitMQ/Kafka.
* **Persistence:** Database-per-service (PostgreSQL, MySQL, MongoDB).
* **Resilience:** Resilience4j (Circuit Breaker, Retry).

## 🛠️ Tech Stack

* **Core:** Java (Spring Boot), NodeJS (NestJS).
* **Business Services:** Order (Java), Inventory (NestJS), Product (Java/Redis).
* **Infra:** Docker, Docker-Compose, GitHub Actions.
* **Storage:** Redis (Cache), SQL & NoSQL.
* **Observability:** ELK Stack, Prometheus & Grafana.

---

## 🏛️ Architect's Philosophy (Tư duy Kiến trúc)

Làm dự án Lab theo tư duy của một **Kiến trúc sư (Architect)** tập trung vào việc "Làm sao để các mảnh ghép kết nối và vận hành bền bỉ?" chứ không chỉ là code tính năng CRUD.

### 1. Infrastructure as Code (Hạ tầng là duy nhất)
* Hệ thống phải có khả năng dựng lại hoàn toàn từ con số 0 chỉ bằng một lệnh: `docker-compose up -d` (từ thư mục `infra`).
* Toàn bộ Database, Message Broker, Search Engine và Identity Provider (Keycloak) được container hóa và cấu hình sẵn mạng nội bộ.

### 2. Integration over Implementation (Kết nối là trọng tâm)
* **Contract First:** Ưu tiên định nghĩa cấu trúc Event/API (ZenWave) trước khi viết logic.
* **Resilience:** Tập trung xử lý **Retry**, **Dead Letter Queue** và **Circuit Breaker** (Resilience4j). Một mảnh vỡ không được phép làm sập toàn bộ bức tranh.
* **Data Consistency:** Xử lý bài toán nhất quán dữ liệu qua Saga Choreography và đồng bộ CQRS giữa Postgres - Elasticsearch.

### 3. Observability is Mandatory (Giám sát là bắt buộc)
* **Traceability:** Mọi request được gắn `correlation_id` xuyên suốt từ Gateway qua Kafka đến ELK Stack.
* **Unified Dashboard:** Theo dõi sức khỏe hệ thống (CPU/RAM/Kafka Lag) qua Prometheus & Grafana.
* **Troubleshooting Playbook:** Quy trình chuẩn để "bắt bệnh" hệ thống. 👉 [Xem chi tiết tại đây](./services/docs/troubleshooting-playbook.md)

### 4. Chaos Engineering (Mô phỏng đổ vỡ)
* Chủ động "kill" service hoặc dừng Kafka để kiểm chứng khả năng tự phục hồi và tính đúng đắn của dữ liệu sau khi hệ thống ổn định trở lại.

---

## 🚀 Research & Development Roadmap

### Phase 1: Foundation & Connectivity — 100%
- [x] **Setup Infrastructure with Docker Compose:** Postgres, Redis, RabbitMQ, Kafka, MongoDB, Keycloak, ELK Stack.
- [x] **API Gateway Implementation:** Spring Cloud Gateway, dynamic routing, rate limiting.
- [x] **Service Discovery:** Netflix Eureka.
- [x] **Centralized Configuration:** Spring Cloud Config API.
- [x] **API Testing & UI:** Triển khai **Swagger/Postman Collection** để kích hoạt và kiểm thử các luồng (Low priority UI).

### Phase 2: Design Patterns & Consistency — 100%
- [x] **Saga Pattern (Choreography):** Lưu đơn PENDING -> Phản hồi từ Kho -> Cập nhật trạng thái.
- [x] **Database per Service:** PostgreSQL (Order) & MongoDB (Inventory).
- [x] **CQRS Pattern:** Tách biệt Write (JPA) và Read (Elasticsearch + ES|QL).
- [x] **Idempotency Consumer:** Đảm bảo không xử lý lặp sự kiện trong Kafka.

### Phase 3: Reliability & Performance — 100% ✅

- [x] **Resilience4j Integration:** Circuit Breaker & Retry cho REST communication.
- [x] **Dead Letter Queue (DLQ):** Triển khai cơ chế cách ly tin nhắn lỗi trên Kafka.
- [x] **Security & Secrets Management:** Mã hóa Secrets và bảo mật Config Server ([Issue #13](https://github.com/viet291993/nexus-microservices-lab/issues/13)).
- [x] **Distributed Caching:** Redis Cache Aside cho Product catalog.
- [x] **Rate Limiting:** Bảo vệ hệ thống từ phía Gateway.
👉 **[Xem chi tiết hướng dẫn vận hành Phase 3 tại đây](./services/docs/phase-3-walkthrough.md)**

### Phase 4: Observability — 20%

- [x] **Centralized Logging (ELK):** Đẩy log tập trung về Kibana qua Logstash.
- [x] **Distributed Tracing:** Tích hợp Zipkin/Tempo để theo dõi hành trình request (correlation_id).
- [ ] **Metrics & Monitoring:** Thiết lập **Grafana Dashboard** mẫu (Prometheus) để quan sát CPU, RAM, Kafka Lag và throughput thực tế.

### Phase 5: CI/CD & Automation — 50%

- [ ] **GitHub Actions:** Tự động hóa quy trình test và build images.
- [x] **Infrastructure as Code:** Bộ script `manage.ps1`/`manage.sh` giúp khởi tạo toàn bộ Lab chỉ với 1 câu lệnh.

---

## 🧠 Lab Notes (Key Learnings)

1. **Connect the dots:** Trọng tâm dự án không nằm ở cú pháp Java/NodeJS mà nằm ở cách điều phối tin nhắn qua Kafka và quản lý trạng thái phân tán.
2. **Standardization:** Việc áp dụng `correlation_id` và chuẩn hóa Log format là "cứu cánh" duy nhất khi hệ thống Microservices trở nên phức tạp.
3. **Troubleshoot before Fix:** Luôn dùng Playbook để xác định nguyên nhân tại Monitor/Log trước khi thay đổi bất kỳ dòng code nào.
