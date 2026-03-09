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

### Phase 4: Observability — 80%

- [x] **Centralized Logging (ELK):** Đẩy log tập trung về Kibana qua Logstash.
- [x] **Distributed Tracing:** Tích hợp Zipkin/Tempo để theo dõi hành trình request (correlation_id).
- [x] **Metrics & Monitoring:** Thiết lập **Grafana Dashboard** mẫu (Prometheus) để quan sát CPU, RAM, Kafka Lag và throughput thực tế.

### Phase 5: CI/CD & Automation — 50%

- [ ] **GitHub Actions:** Tự động hóa quy trình test và build images.
- [x] **Infrastructure as Code:** Bộ script `manage.ps1`/`manage.sh` giúp khởi tạo toàn bộ Lab chỉ với 1 câu lệnh.

---

## 🧠 Tổng hợp Bài học & Kinh nghiệm (Key Learnings & Achievements)

Thông qua quá trình xây dựng **Nexus Microservices Lab** từ con số 0 đến tự động hóa hoàn toàn, dưới đây là những kiến thức thực tiễn có giá trị nhất đã được đúc kết:

### 1. Giải quyết bài toán "Bất đồng ngôn ngữ" (Polyglot Architecture)
- Quản lý 2 hệ sinh thái **Java (Spring Boot)** và **NodeJS (NestJS)** hoạt động cùng nhau.
- **Bài học:** Việc sử dụng một "Nguồn chân lý" chung (Source of Truth) cho Event Schema (`shared/event-schemas/asyncapi.yaml`) kết hợp công cụ sinh mã (`@asyncapi/modelina`) đã giúp giải quyết triệt để rủi ro lệch chuẩn dữ liệu (Schema Drift) giữa hai ngôn ngữ.

### 2. Làm chủ các Mẫu thiết kế (Microservices Patterns)
- **Saga Pattern (Choreography):** Thành thạo cách chia nhỏ một Transaction nguyên khối thành các Local Transactions. Học được tầm quan trọng của **Compensation Logic** (Hoàn tác) khi có lỗi xảy ra ở các bước tiếp theo (ví dụ: trừ kho thất bại phải báo hủy đơn hàng).
- **CQRS & CDC (Change Data Capture):** Tách biệt Database Đọc/Ghi. Áp dụng **Debezium** đọc WAL Log từ PostgreSQL, tự động đẩy qua Kafka Connect (Sink Connector) vào Elasticsearch. Xử lý triệt để Technical Debt của việc code "đồng bộ tay" (Sync thủ công) giữa các database.
- **Cache-Aside Pattern:** Ứng dụng Redis để tăng tốc API Đọc, cũng như xử lý các vấn đề Invalidation khi dữ liệu gốc bị thay đổi cấp thời.

### 3. Thiết kế hệ thống chịu lỗi (Fault Tolerance & Resilience)
- Hệ thống phân tán *rất dễ sập cục bộ*. Việc ứng dụng **Circuit Breaker** (Resilience4j) đã ngăn chặn lỗi lan truyền (Cascading Failure).
- Bài học đắt giá về **Dead Letter Queue (DLQ)**: Mọi Consumer trong Kafka đều phải có DLQ để cách ly các "Poison Pill" (Tin nhắn lỗi format, lỗi logic) tránh việc hệ thống liên tục retry vô hạn làm nghẽn luồng xử lý chính.
- Bảo vệ Gateway bằng thuật toán **Token Bucket** Rate Limiting.

### 4. Giám sát hệ thống toàn diện (Observability is Mandatory)
- Trong một mớ bòng bong hàng chục container, nếu hệ thống "mù" thì không thể trace lỗi.
- **Bài học:** Bắt buộc phải có **Correlation ID** đính kèm mọi request/log.
- Trải nghiệm thực tế với **ELK Stack (Logstash, Kibana)** để gom log tập trung, **Zipkin** để xem Request Timeline (Tracing), và **Prometheus/Grafana** để đo hiệu suất phần cứng & Kafka Lag. Thiết lập thành công ILM (Index Lifecycle) để tự dọn dẹp log cũ lưu trữ.

### 5. Tự động hóa Hạ tầng & CI/CD (DevOps Mindset)
- **Bảo mật:** Loại bỏ việc lưu Secret dạng plain text trên Config Repo bằng **Spring Cloud Config JCE Encryption** (`{cipher}`).
- **Infrastructure Automation:** Phát triển `cdc-provisioner` tự động thiết lập hạ tầng khi hệ thống vừa "boot" thay vì phải chạy các lệnh `curl` bằng tay.
- **CI/CD vững chắc:** Hoàn thiện pipeline GitHub Actions:
  - Docker Buildx sử dụng `driver: docker-container` và cache `mode=min` để tránh vỡ giới hạn 10GB của GitHub.
  - Pin cứng (Hard-pin) SHA của các Github Action chống hiểm họa supply-chain.
  - Xây dựng workflow **Zero-Downtime Deployment** tới máy chủ thực tế thông qua SSH và **Docker Swarm Rolling Update**.

***"Microservices không phải là về code, mà là về cách quản lý sự phân tán, kết nối, và tự phục hồi của hàng chục tiến trình nhỏ lẻ!"***
