# 🌐 Nexus: Services Ecosystem

Thư mục này là trái tim lưu trữ toàn bộ các ứng dụng Microservices (Business Logic) thực dụng của dự án. 

Tại Phase 2 này, dự án đang mô phỏng cấu trúc **Đa ngôn ngữ** (Polyglot Microservices), kết nối giao dịch phân tán (Saga Pattern) qua Message Broker Kafka và xử lý dòng tách biệt Đọc/Ghi (CQRS).

---

## 📂 Danh sách Services

*   **`order-service`**: Microservice bằng `Java (Spring Boot 3)` xử lý đặt hàng, giao dịch ACID, nối kết PostgreSQL.
*   **`inventory-service`**: Microservice bằng `NodeJS (NestJS)` xử lý số lượng và danh mục con kho hàng, nối MongoDB lấy tính linh hoạt (JSON Document).

---

## 📖 Tài Liệu (Documentation)

Chi tiết kiến trúc, Tech Stack và quá trình phát triển (Roadmap) của các ứng dụng này được đặt riêng tại chuỗi tài liệu bên trong thư mục `docs/`.

👉 **[Xem chi tiết Thiết kế Kiến Trúc (Architecture) Phase 2 TẠI ĐÂY](./docs/architecture.md)**
