# Nexus: Services Ecosystem (Polyglot Microservices)

Thư mục này là trung tâm chứa các dịch vụ kinh doanh (Business Logic) thực thụ của dự án Nexus Microservices.

---

## 🏗️ Kiến trúc Polyglot (Đa ngôn ngữ)
Để phản ánh sát nhất môi trường Microservices linh hoạt của các công ty công nghệ lớn, Phase 2 ứng dụng tư tưởng Polyglot:

1. **Order Service (Java - Spring Boot 3):** 
   - **Mục tiêu:** Xử lý tác vụ tạo đơn hàng liên quan đến dòng tiền, yêu cầu Transaction cục bộ cực kỳ khắt khe bám rễ vào CSDL quan hệ chặt chẽ.
   - **Database:** PostgreSQL.
   - **Vai trò trong Saga:** Là người kích hoạt giao dịch (Saga Initiator), ném sự kiện `ORDER_CREATED` sang Kafka cho các bên khác xử lý bù hoặc đi tiếp.

2. **Inventory Service (NodeJS - NestJS):**
   - **Mục tiêu:** Xử lý kho hàng. Quản lý số lượng tồn kho của hàng ngàn dạng sản phẩm có thuộc tính phi cấu trúc cực kỳ nhanh chóng bằng Non-blocking I/O của Node.
   - **Database:** MongoDB.
   - **Vai trò trong Saga:** Lắng nghe Kafka 24/7 (Saga Participant). Trừ kho thành công thì im lặng, gặp lỗi thì hét lên `INVENTORY_FAILED` vào Kafka để Order Service quay về cập nhật trạng thái đơn thành CANCELLED.

---

## 🛠️ Quy trình Development Phase 2 (Saga Pattern & CQRS)

### Bước 1: Khởi tạo Base (Đã hoàn thành)
- Dùng `spring initializr` tạo khung Spring Boot cho `order-service` (Kẹp sẵn Web, Kafka, JPA, Config, Eureka).
- Dùng `npx @nestjs/cli` tạo khung NestJS cho `inventory-service` (Kẹp sẵn Kafka transport, Config, Mongoose).

### Bước 2: Cấu hình Connectivity (Database & Config Server)
- Kết nối `order-service` vào PostgreSQL.
- Kết nối `inventory-service` vào MongoDB.
- Móc cả hai nhánh trên vào Spring Cloud Config Server (để lấy file `.yml`/`.json` chứa mật khẩu Database trung tâm).

### Bước 3: Build lõi Kafka (Core Message Driven)
- Cấu hình Topic trên máy Kafka (vd: `saga-orders-topic`).
- Viết các Kafka Producer (Gửi) và Consumer (Nhận) cơ bản ở cả 2 service.
- Test ping qua lại để đảm bảo 2 service nói chuyện được bằng Message thay vì gọi HTTP REST API.

### Bước 4: Viết Saga Choreography Logic & CQRS
- Mô phỏng Data dữ liệu thật. Order chém vô Table sinh đơn Pending -> Quăng Kafka -> Inventory trừ hàng -> Failed quăng Kafka về Order.
- CQRS: Xây tiếp Elasticsearch/Redis hứng cục Kafka để tạo View phục vụ luồng Read.
