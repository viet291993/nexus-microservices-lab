# 📔 Troubleshooting Playbook: Nexus Microservices

Tài liệu này cung cấp quy trình chuẩn để truy vết và xử lý sự cố trong môi trường Microservices của dự án Nexus, tập trung vào tư duy của một **Architect**.

---

## 0. Tiền đề để truy vết (Prerequisites)

Để Playbook này hiệu quả, hệ thống đã được cấu hình các chuẩn sau:
- **`correlation_id` / `traceId`**: Xuất hiện xuyên suốt trong Log (MDC), HTTP Header (`X-Correlation-Id`), và Kafka Message Headers.
- **Observability Stack**: Grafana (Metrics), Prometheus, Kibana (Logs), Tempo (Tracing).

---

## 1. Grafana báo đỏ: Chọn đúng "Triệu chứng"

Khi có sự cố, bước đầu tiên là xác định nhóm triệu chứng tại Grafana:

*   **Triệu chứng A: API 5xx tăng** -> Lọc theo `service`/`route` (Gateway → Order → Inventory).
*   **Triệu chứng B: Latency p95/p99 tăng** -> Xem độ trễ tăng ở Gateway hay tại nội bộ Service.
*   **Triệu chứng C: Kafka Consumer Lag tăng** -> Kiểm tra xem Topic/Partition nào đang bị ứ đọng, Consumer Group nào bị treo.
*   **Triệu chứng D: Resource (Memory/CPU) đạt ngưỡng** -> Container có dấu hiệu OOM (Out of Memory) hoặc Restart loop.

**Kết quả:** Xác định được **Service nghi ngờ** + **Khoảng thời gian (Time Window)** xảy ra sự cố.

---

## 2. Kibana: Khoanh vùng lỗi

Sử dụng Time Window từ Grafana để tìm kiếm trong Kibana:
1.  Chọn Index: `order-service-*`, `inventory-service-*`...
2.  Lọc log: `level: ERROR OR WARN`.
3.  Tìm kiếm các "mẫu lỗi" xuất hiện nhiều nhất (Stacktrace, Exception message).

---

## 3. Truy vết bằng `correlation_id` (The Deep Dive)

Lấy một `correlation_id` từ log lỗi và thực hiện truy vết chuỗi sự kiện:

1.  **Tại API Gateway**: Query `correlation_id: "<id>" AND service: "api-gateway"`. Xác định request gốc và status code trả về.
2.  **Tại Order Service**: Kiểm tra xem `ORDER_CREATED` có được tạo không? Trạng thái chuyển từ `PENDING` sang gì?
3.  **Tại Inventory Service**: Đã nhận được Event chưa? Việc trừ kho (Deduct Stock) thành công hay thất bại?
4.  **Luồng phản hồi**: Kiểm tra `Order Service` có nhận được Event phản hồi từ Kho để hoàn tất Saga hay không?

---

## 4. Phân loại và Xử lý theo Nhóm lỗi

### 🛑 Nhóm 1: Kafka / Integration

- **Dấu hiệu**: Consumer lag tăng, Broker disconnect, Timeout khi produce.
- **Hành động**:
    1. Kiểm tra tình trạng Container Kafka.
    2. Kiểm tra **Dead Letter Queue (DLQ)** topic (VD: `inventory-events-topic.dlq`). Nếu có tin nhắn trong này, nghĩa là logic xử lý đã thất bại sau nhiều lần Retry.
    3. Phân tích nguyên nhân trong tin nhắn chết (Header chứa lỗi) và nạp lại (Reprocess) nếu cần.

### 🛑 Nhóm 2: Idempotency / Duplicate

- **Dấu hiệu**: Cùng một `orderId` nhưng xử lý nhiều lần, trạng thái đơn hàng bị "flip-flop" (đang Confirmed lại về Cancelled).
- **Hành động**: Kiểm tra Idempotency Key, đảm bảo quy tắc "Final State không bị ghi đè".

### 🛑 Nhóm 3: Resource / Overload

- **Dấu hiệu**: CPU 100%, GC Pause dài, lỗi "OutOfMemoryError".
- **Hành động**: Tune lại JVM Heap/Resource limits, giảm số lượng tin nhắn xử lý đồng thời (Concurrency) của Consumer.

### 🛑 Nhóm 4: Data Sync (CQRS Pipeline)

- **Dấu hiệu**: Tìm kiếm trên Elasticsearch không thấy dữ liệu mới hoặc bị sai lệch so với PostgreSQL.
- **Hành động**: Kiểm tra Sync Listener, Elasticsearch Bulk Reject, hoặc Mapping Template lỗi.

---

## 5. Kết luận sự cố (Post-mortem)

Mỗi lần xử lý xong, hãy ghi lại 5 dòng:
1.  **Impact**: Ảnh hưởng đến bao nhiêu % request? API nào?
2.  **Root Cause**: Thuộc nhóm lỗi nào ở trên?
3.  **Evidence**: Đường dẫn tới Dashboard Grafana/Kibana lúc xảy ra lỗi.
4.  **Short-term Fix**: Đã làm gì để hệ thống hoạt động lại (Restart, Clear queue...)?
5.  **Prevention**: Cần thêm Alert gì, hay cải tiến code (Circuit Breaker, DLQ) hiệu quả.

---

**💡 Lời khuyên của Architect:**
> "Hệ thống chưa hoàn thiện nếu bạn không thể giải thích tại sao nó chết." - Luôn luôn trace trước khi fix.
