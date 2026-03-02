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

* **Core:** Java (Spring Boot), NodeJS (NestJS).
* **Infra:** Docker, Docker-Compose, GitHub Actions.
* **Storage:** Redis (Cache), SQL & NoSQL.
* **Observability:** ELK Stack, Prometheus & Grafana.

---

## 🧠 Lab Notes (Học được gì?)

### 1. Giải mã Architecture qua "Docs & AI"

Thay vì học vẹt, tôi kết hợp giữa **Official Documentation** để nắm gốc và **AI** để phản biện kiến trúc. Tôi dùng AI như một *Senior Partner* để brainstorm các phương án như *Saga Orchestration vs Choreography*, sau đó tự tay verify lại bằng code thực tế.

### 2. Tư duy "Connect the dots"

Với 5.5 năm làm nghề, mục tiêu của tôi ở dự án này không phải là học cú pháp, mà là học cách **kết nối**. Từ việc đóng gói Docker, điều phối tin nhắn qua Kafka đến việc tracking log tập trung – dự án này là cách tôi hệ thống hóa lại bức tranh Backend tổng thể.

### 3. Tự học là kỹ năng sinh tồn

Công nghệ thay đổi theo tuần, nhưng tư duy giải quyết vấn đề là bất biến. Việc tự setup dàn hạ tầng từ con số 0 giúp tôi rèn luyện khả năng thích nghi nhanh với bất kỳ Stack công nghệ nào.

---

🛠️ Research & Development Roadmap
Phase 1: Foundation & Connectivity (Hạ tầng & Kết nối)
[ ] Setup Infrastructure with Docker Compose: Cấu hình tập trung Postgres, Redis, RabbitMQ và Kafka.

[ ] API Gateway Implementation: Thiết lập Spring Cloud Gateway, cấu hình dynamic routing và custom filters.

[ ] Service Discovery: Triển khai Netflix Eureka hoặc Consul để quản lý danh sách service.

[ ] Centralized Configuration: Sử dụng Spring Cloud Config để quản lý biến môi trường tập trung.

Phase 2: Design Patterns & Consistency (Kiến trúc & Tính nhất quán)
[ ] Saga Pattern (Choreography): Nghiên cứu và triển khai cơ chế rollback dữ liệu giữa Order Service và Inventory Service thông qua Message Broker.

[ ] Database per Service: Tách biệt database hoàn toàn và xử lý bài toán join dữ liệu ở tầng Application.

[ ] CQRS Pattern: Thử nghiệm tách biệt luồng Read và Write để tối ưu hiệu năng truy vấn (Product Catalog).

[ ] Idempotency Consumer: Đảm bảo xử lý tin nhắn từ Broker không bị trùng lặp dữ liệu.

Phase 3: Reliability & Performance (Độ tin cậy & Hiệu suất)
[ ] Resilience4j Integration: Triển khai Circuit Breaker và Retry cho các luồng giao tiếp synchronous (REST).

[ ] Distributed Caching: Sử dụng Redis Cache Aside pattern để giảm tải cho database chính.

[ ] Rate Limiting: Cấu hình giới hạn request tại Gateway để chống spam và bảo vệ hệ thống.

Phase 4: Observability (Khả năng giám sát)
[ ] Centralized Logging (ELK): Đẩy log từ tất cả service về Elasticsearch thông qua Logstash.

[ ] Distributed Tracing: Tích hợp Zipkin hoặc Sleuth để theo dõi hành trình của một request đi qua nhiều service.

[ ] Metrics Dashboard: Thiết lập Prometheus và Grafana để quan sát chỉ số CPU, RAM và throughput thực tế.

Phase 5: CI/CD & Automation
[ ] GitHub Actions: Tự động hóa quy trình chạy Unit Test và Build Docker Image.

[ ] Infrastructure as Code: Viết script để setup nhanh môi trường dev chỉ với một câu lệnh.
