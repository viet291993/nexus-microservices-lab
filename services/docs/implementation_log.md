# CQRS Implementation Log - Order Service

Tài liệu này ghi lại quá trình triển khai thực thực tế, các lỗi phát sinh và giải pháp xử lý.

## 📅 Nhật ký chi tiết

### 🟢 2026-03-05: Khởi động & Fix Testcontainers

#### 1. Lỗi kết nối Docker (macOS/Colima/Docker Desktop)
- **Vấn đề:** Testcontainers không tìm thấy Docker socket, báo lỗi `Could not find a valid Docker environment`.
- **Nguyên nhân:** Đường dẫn socket `/var/run/docker.sock` trên macOS thường yêu cầu quyền root hoặc cấu hình `DOCKER_HOST` cụ thể.
- **Giải pháp:** 
    - Cấu hình file `~/.testcontainers.properties`:
      ```properties
      docker.host=unix:///var/run/docker.sock
      docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
      ```
    - Chạy test với biến môi trường `DOCKER_HOST="unix:///var/run/docker.sock"`.

#### 2. Lỗi Mismatch Version Elasticsearch (Java Client 9.x vs Server 8.x)
- **Vấn đề:** Khi app khởi động, `OrderSearchRepository` báo lỗi `node: http://localhost:9200/, status: 400, [es/indices.exists] Expecting a response body, but none was sent`.
- **Nguyên nhân:** Spring Boot 4.x sử dụng `elasticsearch-java` client bản 9.2.x. Client này gửi request `HEAD` (indices.exists) mà server ES 8.12.x phản hồi không đúng mong đợi của client, dẫn đến lỗi 400.
- **Giải pháp:** 
    - Nâng cấp Elasticsearch Image lên **9.2.0** trong `docker-compose.yml` và `BaseSagaIntegrationTest.java`.
    - **Lưu ý:** Nâng cấp bản major (8 -> 9) yêu cầu xóa data cũ: `docker compose down -v`.

#### 3. Lỗi "Invalid media-type value on headers [Content-Type, Accept]"
- **Vấn đề:** Khi lưu `OrderDocument` vào ES, nảy sinh lỗi `media_type_header_exception`.
- **Nguyên nhân:** Do version mismatch hoặc cấu hình Jackson mapper trong `elasticsearch-java`.
- **Giải pháp:** Đồng nhất version lên 9.2.0 đã giải quyết được vấn đề này.

#### 4. Lỗi "createIndex = true" trong Spring Data ES
- **Vấn đề:** Khi bean initialization, Spring Data ES cố gắng kiểm tra index tồn tại. Nếu index chưa có, nó có thể gây ra lỗi 400 với client 9.x.
- **Giải pháp:** Tạm thời tắt tự động tạo index bằng `@Document(indexName = "orders", createIndex = false)`. Index sẽ được tạo manually hoặc qua event sync đầu tiên (tùy thiết kế).

---

## 🛠 Trạng thái hiện tại (Current Status)
- **Infrastructure:** OK (ES 9.2.0, PostgreSQL, Kafka).
- **Application:** OK (Vượt qua phase ApplicationContext load).
- **Testing:** Đang debug logic trong `SagaE2EIntegrationTest`.
    - `shouldCompleteSagaSuccessfully`: Fail ở bước Verify dữ liệu ES.
    - `shouldRollbackSaga`: Lỗi `The given id must not be null`.

## 📌 Tiếp nhiệm (Next Action)
- Sửa logic test trong `SagaE2EIntegrationTest.java` để hoàn tất Verify.
- Kiểm tra tính Idempotent của `OrderSyncEventListener`.
