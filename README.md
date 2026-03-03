# Nexus: Microservices Exploration Lab

*A modern playground for decoding distributed systems and backend architectures.*

---

## 🏗️ Architecture

* **API Gateway:** Spring Cloud Gateway (Rate limiting, Dynamic Routing).
* **Auth:** Stateless JWT & OAuth2.
* **Communication:**
    * **Sync:** REST/gRPC for consistency.
    * **Async:** Event-driven via RabbitMQ/Kafka.
* **Persistence:** Database-per-service (PostgreSQL, MySQL, MongoDB).
* **Resilience:** Resilience4j (Circuit Breaker, Retry).

## 🛠️ Tech Stack

* **Core:** Java (Spring Boot), NodeJS (NestJS), Python (FastAPI).
* **Infra:** Docker, Docker-Compose, GitHub Actions.
* **Storage:** Redis (Cache), SQL & NoSQL.
* **Observability:** ELK Stack, Prometheus & Grafana.

---

## 🧠 Lab Notes (Key Learnings)

### 1. Giải mã Architecture qua "Docs & AI"
Thay vì học vẹt, phương pháp cốt lõi là kết hợp giữa **Official Documentation** để nắm vững nền tảng và **AI** để phản biện kiến trúc. AI đóng vai trò như một *Senior Partner* để brainstorm các phương án (như *Saga Orchestration vs Choreography*), sau đó tiến hành xác thực bằng code thực tế.

### 2. Tư duy "Connect the dots"
Trọng tâm của dự án không nằm ở việc học cú pháp mà là kỹ năng **kết nối hệ thống**. Từ việc đóng gói Docker, điều phối tin nhắn qua Kafka đến quản lý log tập trung – đây là quá trình hệ thống hóa toàn diện bức tranh Backend.

### 3. Tự học là kỹ năng sinh tồn
Công nghệ thay đổi liên tục, nhưng tư duy giải quyết vấn đề là bất biến. Việc tự thiết lập toàn bộ hạ tầng từ con số 0 giúp rèn luyện khả năng thích nghi nhanh với bất kỳ Tech Stack nào trong môi trường thực tế.

---

## 🛠️ Research & Development Roadmap

### Phase 1: Foundation & Connectivity (Hạ tầng & Kết nối)
- [ ] **Setup Infrastructure with Docker Compose:** Cấu hình tập trung Postgres, Redis, RabbitMQ và Kafka.
- [x] **API Gateway Implementation:** Thiết lập Spring Cloud Gateway, cấu hình dynamic routing và custom filters.
- [x] **Service Discovery:** Triển khai Netflix Eureka hoặc Consul để quản lý danh sách service.
- [ ] **Centralized Configuration:** Sử dụng Spring Cloud Config quản lý biến môi trường tập trung.

### Phase 2: Design Patterns & Consistency (Kiến trúc & Tính nhất quán)
- [ ] **Saga Pattern (Choreography):** Triển khai cơ chế rollback dữ liệu giữa các service thông qua Message Broker.
- [ ] **Database per Service:** Tách biệt database hoàn toàn và xử lý bài toán truy vấn dữ liệu ở tầng Application.
- [ ] **CQRS Pattern:** Thử nghiệm tách biệt luồng Read và Write để tối ưu hiệu năng.
- [ ] **Idempotency Consumer:** Đảm bảo xử lý tin nhắn từ Broker không bị trùng lặp dữ liệu.

### Phase 3: Reliability & Performance (Độ tin cậy & Hiệu suất)
- [x] **Resilience4j Integration:** Triển khai Circuit Breaker và Retry cho giao tiếp synchronous (REST).
- [ ] **Distributed Caching:** Sử dụng Redis Cache Aside pattern để giảm tải cho database chính.
- [x] **Rate Limiting:** Cấu hình giới hạn request tại Gateway để bảo vệ hệ thống.

### Phase 4: Observability (Khả năng giám sát)
- [ ] **Centralized Logging (ELK):** Đẩy log từ tất cả service về Elasticsearch thông qua Logstash.
- [ ] **Distributed Tracing:** Tích hợp Zipkin hoặc Sleuth để theo dõi hành trình request.
- [ ] **Metrics Dashboard:** Thiết lập Prometheus và Grafana để quan sát CPU, RAM và throughput thực tế.

### Phase 5: CI/CD & Automation
- [ ] **GitHub Actions:** Tự động hóa quy trình chạy Unit Test và Build Docker Image.
- [ ] **Infrastructure as Code:** Viết script để thiết lập nhanh môi trường phát triển chỉ với một câu lệnh.
