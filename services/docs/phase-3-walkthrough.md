# Hướng dẫn hoàn tất Phase 3 (Reliability & Performance)

Tài liệu này tổng hợp toàn bộ các tính năng đã triển khai trong Phase 3 nhằm tăng cường tính ổn định, bảo mật và hiệu năng cho hệ thống Nexus.

---

## 1. Dead Letter Queue (DLQ) & Retry Strategy
Cơ chế giúp hệ thống tự phục hồi và cách ly các lỗi xử lý dữ liệu bất đồng bộ.

- **Cấu hình:** Tập trung tại `config-server` cho `order-service`.
- **Luồng hoạt động:**
  1. `order-service` tiêu thụ message từ Kafka.
  2. Nếu xảy ra Exception, Spring Cloud Stream sẽ thực hiện **Retry 3 lần** với Exponential Backoff (1s, 2s, 4s).
  3. Sau 3 lần thất bại, message được đẩy về topic `.dlq` (Ví dụ: `inventory-events-topic.dlq`).
- **Kiểm chứng:** Đã có bài test tích hợp tại `KafkaDLQIntegrationTest.java`. Bài test này giả lập lỗi DB và kiểm tra message trong DLQ topic.

## 2. Distributed Caching (Redis) & Product Service
Tăng tốc độ truy vấn danh mục sản phẩm và giảm tải cho Database chính.

- **Microservice mới:** `product-service` (Java Spring Boot + MongoDB).
- **Mẫu thiết kế:** **Cache-Aside Pattern**.
  - **Read:** Check Redis -> Hit (Return) / Miss (Load MongoDB -> Write Redis -> Return).
  - **Write/Update/Delete:** Thực hiện trên DB và tự động xóa/cập nhật Cache tương ứng (`CacheEvict`, `CachePut`).
- **Base Image:** Sử dụng `eclipse-temurin:17-jre-jammy` để vá các lỗ hổng bảo mật (High CVEs) và tối ưu hiệu suất.

## 3. Config Server Security & Secrets Management
Bảo vệ tài nguyên cấu hình và mã hóa thông tin nhạy cảm.

- **Authentication:** Basic Auth qua biến môi trường (ví dụ: `${CONFIG_SERVER_USERNAME}` / `${CONFIG_SERVER_PASSWORD}`), không hard-code trong tài liệu.
- **JCE Encryption:** Cho phép mã hóa các mật khẩu trong file YAML.
- **Cách sử dụng:**
  1. Mã hóa: `curl -u ${CONFIG_SERVER_USERNAME}:${CONFIG_SERVER_PASSWORD} localhost:8888/encrypt -d "your_password"`.
  2. Cấu hình: Thay giá trị bằng `{cipher}chuỗi_đã_mã_hóa`.

## 4. Troubleshooting Playbook cho Phase 3
Khi hệ thống có tin nhắn trong DLQ:
1. Kiểm tra log của `order-service` để tìm `correlation_id` của tin nhắn lỗi.
2. Xác định nguyên nhân (Lỗi logic hay lỗi hạ tầng tạm thời).
3. Sau khi fix, có thể nạp lại tin nhắn từ DLQ vào topic chính để xử lý lại.

---
**💡 Ghi chú của Architect:**
> "Reliability không phải là không bao giờ lỗi, mà là cách chúng ta kiểm soát và hồi phục sau lỗi."
