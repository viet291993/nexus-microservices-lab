# Nexus: Services Architecture

Tài liệu này mô tả chi tiết kiến trúc chuyên sâu của hệ sinh thái các dịch vụ kinh doanh (Business Services) và cách chúng giao tiếp phân tán trong dự án Nexus Microservices.

---

## 🏗️ Kiến trúc Polyglot (Đa ngôn ngữ)
Để phản ánh sát nhất môi trường Microservices linh hoạt của các công ty công nghệ lớn, Phase 2 ứng dụng tư tưởng Polyglot:

### 1. Order Service (Nhân vật chính)
- **Tech Stack:** `Java` (Spring Boot 3), `Spring Data JPA`
- **Database:** PostgreSQL (vì đơn hàng dính tới tiền bạc và quan hệ nên buộc phải có tính toàn vẹn ACID cực mạnh của Relational Database).
- **Vai trò trong Saga:** Là người kích hoạt giao dịch (Saga Initiator), tiếp nhận request tạo đơn, lưu bảng Pending, rồi ném sự kiện `ORDER_CREATED` sang Message Broker (Kafka) cho các bên khác xử lý bù hoặc đi tiếp.

### 2. Inventory Service (Nhà kho)
- **Tech Stack:** `NodeJS` (NestJS), `@nestjs/microservices`
- **Database:** MongoDB (Sản phẩm thường có cấu trúc phi quan hệ, Nested json linh hoạt).
- **Vai trò trong Saga:** Lắng nghe kênh Kafka 24/7 (Saga Participant). Trừ kho thành công thì im lặng tiếp tục luồng, gặp lỗi thì quăng sự kiện `INVENTORY_FAILED` vào Kafka để Order Service quay về cập nhật trạng thái đơn thành CANCELLED.

---

## 🛠️ Quy trình Development (Saga Pattern & CQRS)

### ✅ Bước 1: Khởi tạo Base (Scaffold) — HOÀN THÀNH
- Thiết lập khung Spring Boot cho `order-service` (JPA, Kafka, Eureka Client, Config Client).
- Thiết lập khung NestJS cho `inventory-service` (Mongoose, Kafkajs).

### ✅ Bước 2: Cấu hình Connectivity (Database & Config Server) — HOÀN THÀNH
- Kết nối `order-service` vào PostgreSQL qua file `config-repo/order-service.yml`.
- Kết nối `inventory-service` vào MongoDB qua file `.env`.
- Docker Compose đã chạy thành công toàn bộ hạ tầng (Kafka, Postgres, Mongo, Redis...).

### ✅ Bước 3: Build lõi Message Driven (Broker Kafka) — HOÀN THÀNH
- Tự động tạo Topic `order-events-topic` (3 partitions) trên Kafka Broker.
- **Order Service (Java):** Kafka Producer gửi `OrderEvent` bất đồng bộ. 👉 [Chi tiết](./order-service-kafka.md)
- **Inventory Service (NestJS):** Kafka Consumer nhận event, trừ kho MongoDB, gửi phản hồi. 👉 [Chi tiết](./inventory-service-kafka.md)
- Luồng Saga hoàn chỉnh: `ORDER_CREATED` → trừ kho → `INVENTORY_CONFIRMED / FAILED` → rollback/confirm.

### ✅ Bước 4a: Saga Choreography Logic — HOÀN THÀNH
- Tạo `OrderEntity` (JPA) và `OrderRepository` để lưu đơn hàng vào PostgreSQL.
- Nâng cấp `OrderController`: Lưu đơn PENDING vào DB trước → gửi Kafka → trả 202 Accepted.
- Nâng cấp `InventoryResponseConsumer`: Nhận phản hồi Saga → tìm đơn trong DB → cập nhật CONFIRMED hoặc CANCELLED (Compensating Transaction / Rollback).
- Thêm `GET /api/v1/orders` để kiểm tra trạng thái đơn hàng sau khi Saga chạy xong.

### 🔲 Bước 4b: CQRS Pattern — ĐANG CHỜ
- Triển khai luồng đọc dữ liệu từ một cơ sở dữ liệu riêng (Elasticsearch/Redis).
