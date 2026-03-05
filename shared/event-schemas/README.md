# Shared Event Schemas (AsyncAPI)

Thư mục này đóng vai trò là **Nguồn Chân Lý (Source of Truth)** duy nhất cho toàn bộ giao tiếp Event-Driven (Kafka) giữa các Microservices có ngôn ngữ lập trình khác nhau (Polyglot Architecture) trong hệ thống **Nexus Microservices Lab**.

---

## 🏗️ Kiến trúc Polyglot

Dự án hiện tại bao gồm:
1. **Order Service**: Java (Spring Boot)
2. **Inventory Service**: TypeScript (NestJS)

Vì không thể chia sẻ các lớp dữ liệu trực tiếp dưới dạng `.jar` hay package `.js`, chúng ta sử dụng **AsyncAPI Specification** để làm hợp đồng trung gian (Contract).

## 📄 File Thiết kế Chính: `asyncapi.yaml`

Tệp `asyncapi.yaml` định nghĩa toàn bộ luồng Saga Choreography:
- **Topics** Kafka mà hệ thống sử dụng (`order-events-topic`, `inventory-events-topic`).
- **Events** được truyền tải (`OrderCreated`, `InventoryConfirmed`, `InventoryFailed`).
- **Cấu trúc dữ liệu** (Schema/DTO) của mỗi Event (yêu cầu các trường dữ liệu nào, kiểu dữ liệu là gì).

## 🚀 Hướng dẫn Sinh Code (Code Generation)

Mục đích của file Schema này là để tự động sinh ra mã nguồn (Code Generation) cho từng service, nhằm đảm bảo **Data Consistency** tuyệt đối.

### ☕ Cho Java Developers (Order Service)

**Công cụ đề xuất**: AsyncAPI Generator HTTP Server hoặc tích hợp trực tiếp `jsonschema2pojo` (do payload được viết theo chuẩn JSON Schema).

Quy trình chuẩn:
1. Đọc mô hình `OrderEventPayload` từ `asyncapi.yaml`.
2. Generator sẽ tạo ra một lớp Java Record:
   ```java
   public record OrderEvent(String orderId, String productId, Integer quantity, String status, String eventType) {}
   ```

### 🟩 Cho Node.js Developers (Inventory Service TypeScript)

**Công cụ đề xuất**: [AsyncAPI Generator cho TypeScript](https://github.com/asyncapi/ts-nats-template) hoặc các thư viện biến đổi JSON Schema thành TS Interfaces.

Quy trình chuẩn:
1. Đọc payload Schema.
2. Generator sẽ tạo ra một Interface TypeScript để dùng trong NestJS DTO:
   ```typescript
   export interface OrderEventPayload {
     orderId: string;
     productId: string;
     quantity: number;
     status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED';
     eventType: 'ORDER_CREATED' | 'INVENTORY_CONFIRMED' | 'INVENTORY_FAILED';
   }
   ```

## 👁️‍🗨️ Xem tài liệu trực quan

Để xem luồng Saga bằng giao diện đồ họa đẹp mắt:
1. Copy toàn bộ nội dung file `asyncapi.yaml`.
2. Dán vào [AsyncAPI Studio](https://studio.asyncapi.com/).
