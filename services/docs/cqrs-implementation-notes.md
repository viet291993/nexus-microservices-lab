# Ghi chép Triển khai CQRS (Issue #10)

Tài liệu này dùng để theo dõi các bước thực hiện và ghi nhận các vấn đề (issues/blockers) phát sinh trong suốt quá trình áp dụng mẫu CQRS cho Order Service.

## Phase 1: Hạ tầng (Infrastructure)
**Ngày thực hiện:** 2026-03-05
**Công việc:**
1. Thêm cấu hình chạy `elasticsearch` và `kibana` (phiên bản 8.12.2) vào file `infra/docker-compose.yml`.
2. Tắt tính năng bảo mật mặc định (xpack.security) của Elasticsearch để dễ dàng dev ở local (`xpack.security.enabled=false`, `discovery.type=single-node`).
3. Khai báo giới hạn bộ nhớ Java Heap cho ES (`ES_JAVA_OPTS=-Xms512m -Xmx512m`) để tránh ngốn RAM làm crash Docker của máy Mac.
4. Thêm thư viện `spring-boot-starter-data-elasticsearch` vào thẻ `dependencies` trong `pom.xml` của Order Service.

**Các vấn đề gặp phải (Issues encountered):**
- *Đang theo dõi... Lần chạy docker-compose up tới sẽ kiểm tra xem Elasticsearch memory resource có đủ cho local dev không.*

---

## Phase 2: Command Service (Viết và lưu giữ)
- **HOÀN THÀNH**: Database PostgreSQL đóng vai trò Source of Truth.

## Phase 3: Synchronization (Đồng bộ Eventual Consistency bằng Kafka)
- **HOÀN THÀNH**: Sử dụng Kafka CDC và `OrderSyncEventListener` để đồng bộ dữ liệu sang Elasticsearch.

## Phase 4: Query Service (Đọc từ Elasticsearch)
- **HOÀN THÀNH**: API `OrderQueryController` cung cấp khả năng tìm kiếm nâng cao qua Elasticsearch và ES|QL.
