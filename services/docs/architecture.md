# Nexus: Services Architecture (Phase 2)

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

## 🛠️ Quy trình Development Phase 2 (Saga Pattern & CQRS)

### Bước 1: Khởi tạo Base (Scaffold)
- Thiết lập khung Spring Boot cho `order-service` (JPA, Kafka, Eureka Client, Config Client).
- Thiết lập khung NestJS cho `inventory-service` (Mongoose, Kafkajs).

### Bước 2: Cấu hình Connectivity (Database & Config Server)
- Kết nối `order-service` vào PostgreSQL.
- Kết nối `inventory-service` vào MongoDB.
- Móc nối tập trung vào Spring Cloud Config Server (port 8888) hoặc tệp `.env` môi trường cục bộ để lấy dữ liệu kết nối.

### Bước 3: Build lõi Message Driven (Broker Kafka)
- Cấu hình Topic giao dịch trên máy chủ Kafka (vd: `saga-orders-topic`).
- Viết các Kafka Producer (Gửi) và Consumer (Nhận) cơ bản ở cả 2 service.
- Chạy thủ công (Health-check) để đảm bảo hai bên trao đổi Message chéo ngôn ngữ Java-NodeJS thành công.

### Bước 4: Viết Saga Choreography Logic & CQRS
- Mô phỏng Data dữ liệu Order giả định. Order gửi lệnh vào Database -> Quăng luồng Kafka -> Inventory check logic trừ hàng -> Thành công báo OK, Thất bại gọi ngược Rollback.
- CQRS: Triển khai luồng đọc dữ liệu từ một cơ sở dữ liệu riêng (Elasticsearch/Redis).
