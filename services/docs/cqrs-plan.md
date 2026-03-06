# CQRS Implementation Plan - Order Service

Tài liệu này chi tiết hóa kế hoạch triển khai mẫu thiết kế CQRS cho Order Service, tách biệt luồng ghi (PostgreSQL) và luồng đọc (Elasticsearch).

## 1. Phân tích & So sánh các giải pháp Read-DB

| Tiêu chí | Redis | PostgreSQL (Materialized View) | Elasticsearch (Đề xuất) |
| :--- | :--- | :--- | :--- |
| **Tốc độ đọc** | Cực nhanh (In-memory) | Trung bình | Rất nhanh (Distributed Index) |
| **Khả năng Search** | Hạn chế (Key-Value) | Tốt (SQL) | **Xuất sắc** (Full-text search, Filter) |
| **Mở rộng** | Tốt | Phụ thuộc DB chính | Cực tốt (Horizontal Scaling) |
| **Độ phức tạp** | Thấp | Rất thấp | Trung bình |
| **Use Case** | Cache đơn hàng theo ID | Báo cáo đơn giản | **Hệ thống E-commerce / Catalog** |

**=> Lựa chọn: Elasticsearch** để hỗ trợ tra cứu lịch sử đơn hàng với nhiều tiêu chí (status, date range, product name) và khả năng mở rộng trong tương lai.

---

## 2. Kế hoạch triển khai (Tasks)

### Phase 1: Infrastructure & Configuration

- [x] Cấu hình docker-compose để thêm Elasticsearch & Kibana.
- [x] Thêm dependency `spring-boot-starter-data-elasticsearch`.
- [x] Tạo `OrderDocument` (Document mapping sang Elasticsearch).
- [x] Cấu hình Testcontainers hỗ trợ Elasticsearch 9.2.0.

### Phase 2: Command Service (Write)

- [x] Giữ nguyên luồng Saga hiện có trên PostgreSQL.
- [x] Đảm bảo PostgreSQL là **Source of Truth**.

### Phase 3: Synchronization (Eventual Consistency)

- [x] Xây dựng `OrderSyncEventListener` (Read-side):
    - [x] Lắng nghe sự kiện `OrderSyncEvent` (tạo ra từ `OrderCreatedEvent`, v.v.).
    - [x] Cập nhật/chèn dữ liệu vào Elasticsearch Index.
- [x] Xử lý Idempotency sâu: Kiểm tra trạng thái cuối để tránh ghi đè dữ liệu.

### Phase 4: Query Service (Read)

- [x] Tạo `OrderQueryController`.
- [x] Implement API `GET /api/v1/orders/query/search-esql` nâng cao.
- [x] Hoàn tất Integration Test (SagaE2EIntegrationTest).

---

## 3. Luồng dữ liệu (Data Flow)

1. **User** -> `POST /orders` -> **PostgreSQL** (Save) -> **Kafka** (Publish `OrderCreated`).
2. **Kafka** -> **CQRS Sync Manager** (Listen) -> **Elasticsearch** (Save/Update).
3. **User** -> `GET /orders` -> **Elasticsearch** (Fast Read).

---

## 4. Nhật ký triển khai (Implementation Log)

Các vấn đề gặp phải và cách xử lý được ghi lại tại: [implementation_log.md](implementation_log.md)
