# Nexus Saga — Testing Plan

Kế hoạch này tập trung vào **Unit Test + Integration Test cho Saga workflow**, ưu tiên các **failure / compensation scenario** hơn là happy path đơn thuần.

---

## 4.1. Test Infrastructure (Order Service & Inventory Service)

- **Order Service (Java / Spring Boot)**
  - Sử dụng `spring-kafka-test` với `@EmbeddedKafka` để test publish/consume sự kiện mà không cần Kafka thật.
  - Profile `test`:
    - Database: `H2` in‑memory (`jdbc:h2:mem:nexus_test_db`) cho JPA.
    - Tách `application-test.yml` để override URL DB, Kafka bootstrap servers trỏ vào embedded Kafka.
  - Tạo base class `AbstractSagaIntegrationTest`:
    - Annotate với `@SpringBootTest`, `@EmbeddedKafka`.
    - Cung cấp helper:
      - `sendAndWaitForEvent(...)`.
      - `awaitOrderStatus(orderId, expectedStatus)`.

- **Inventory Service (NestJS / Jest)**
  - Sử dụng Jest + `@nestjs/testing`.
  - MongoDB: `mongodb-memory-server` để spin up Mongo in‑memory cho mỗi test suite.
  - Kafka:
    - Đối với **unit test**: mock `ClientKafka` bằng Jest spy (không cần Kafka thật).
    - Đối với **integration test** nâng cao (có Kafka thật/Testcontainers): có thể mở rộng sau nếu cần.
  - Tạo helper `saga-test.helper.ts`:
    - Hàm `createTestingModuleWithMongo()`:
      - Khởi tạo `TestingModule` với `MongooseModule.forRootAsync` trỏ tới Mongo in‑memory.
    - Hàm `mockKafkaClient()`:
      - Trả về object giả có `emit()`, `send()` để assert bằng Jest.

---

## 4.2. Order Service — Event Publisher (Happy Path)

- [x] 4.2.1 Order Creation Triggers Event Publication
- [x] 4.2.2 Event Payload Serialization

### 4.2.1. Order Creation Triggers Event Publication

- **Mục tiêu**: Khi tạo đơn thành công, Order Service phải persist vào DB và publish `OrderCreatedEvent` lên Kafka.
- **Steps** (Junit + EmbeddedKafka):
  1. Seed DB trống (H2).
  2. Gọi `orderService.createOrder(request)` với `productId="P001"`, `quantity=5`.
  3. Assert trong DB:
     - `OrderEntity` tồn tại với `status = PENDING`.
  4. Dùng `KafkaTestUtils.getRecords(...)` để đọc message từ topic `order-events-topic`:
     - Có ít nhất 1 record.
     - Payload chứa `orderId` khớp với entity, `productId="P001"`, `quantity=5`.

### 4.2.2. Event Payload Serialization

- **Mục tiêu**: Đảm bảo JSON event gửi lên Kafka đúng schema, không có field quan trọng bị `null`.
- **Steps**:
  1. Tạo `OrderEntity` đầy đủ field (id, status, timestamps, v.v.).
  2. Gọi trực tiếp `orderEventPublisher.publishOrderCreated(order)`.
  3. Đọc message từ Kafka topic:
     - JSON có đủ property yêu cầu (`orderId`, `productId`, `quantity`, `status`, `timestamp`, …).
     - Không có `null` ở các attribute bắt buộc.

---

## 4.3. Inventory Service — Reservation Failure Scenarios (NestJS/Jest)

- [x] 4.3.1 Insufficient Stock Triggers Failure Event (logic: `InventoryService.deductStock`)
- [x] 4.3.2 Product Not Found (logic: `InventoryService.deductStock`)
- [x] 4.3.3 Zero Quantity Request (logic: `InventoryService.deductStock`)
- [x] 4.3.4 Concurrent Reservation (Race Condition — optional)

### 4.3.1. Insufficient Stock Triggers Failure Event

- **Mục tiêu**: Khi tồn kho không đủ, service phải publish event thất bại, không trừ kho.
- **Steps**:
  1. Dùng `mongodb-memory-server` để seed collection `inventory` với document:
     - `productId="P001"`, `quantity=3`.
  2. Gọi handler Kafka (controller) tương đương:
     - `handleOrderCreated({ orderId: 'O001', productId: 'P001', quantity: 10 })`.
  3. Assert:
     - `kafkaClient.emit('inventory-events-topic', ...)` được gọi với payload có `eventType = INVENTORY_FAILED`.
     - Lý do thất bại truyền trong message (ví dụ `"INSUFFICIENT_STOCK"`).
     - Document `inventory` vẫn giữ `quantity=3` (không bị thay đổi).

### 4.3.2. Product Not Found

- **Mục tiêu**: Không có record trong Mongo → publish failure event, không ném exception.
- **Steps**:
  1. Đảm bảo MongoDB in‑memory **không** có `productId="P999"`.
  2. Gọi `handleOrderCreated` với `productId="P999"`.
  3. Assert:
     - Event thất bại được publish (`INVENTORY_FAILED`, reason `"PRODUCT_NOT_FOUND"`).
     - Hàm handler **không throw**, Jest test pass bình thường.

### 4.3.3. Zero Quantity Request

- **Mục tiêu**: Request `quantity=0` được coi là invalid, không thay đổi kho và publish failure event.
- **Steps**:
  1. Seed item với `quantity=100`.
  2. Gọi `handleOrderCreated({ quantity: 0 })`.
  3. Assert failure event với reason `"INVALID_QUANTITY"` (hoặc message tương đương trong code hiện tại).

### 4.3.4. Concurrent Reservation (Race Condition — optional)

- **Mục tiêu**: Bảo đảm logic trừ kho không tạo giá trị âm khi có 2 request gần đồng thời.
- **Steps (nâng cao, khi logic supports)**:
  1. Seed item `quantity=5`.
  2. Chạy song song hai lời gọi `handleOrderCreated` với `quantity=4` qua `Promise.all`.
  3. Assert:
     - Chỉ một request thành công, một request thất bại.
     - `quantity` cuối cùng trong MongoDB không âm (>=0).

---

## 4.4. Order Compensation Handler — Rollback Logic (Java)

- [x] 4.4.1 Order Fails on Insufficient Inventory (logic: `InventoryResponseConsumer` + `OrderEntity.status`)
- [x] 4.4.2 Compensation Idempotency — Duplicate Failure Event

### 4.4.1. Order Fails on Insufficient Inventory

- **Mục tiêu**: Khi nhận sự kiện thất bại từ Inventory, Order chuyển từ `PENDING` sang trạng thái thất bại.
- **Steps**:
  1. Seed DB với `OrderEntity` `orderId=O001`, `status=PENDING`.
  2. Publish event `InventoryReservationFailedEvent` vào topic phản hồi (ví dụ `inventory-events-topic`) với `orderId=O001`, reason `"INSUFFICIENT_STOCK"`.
  3. Chờ consumer xử lý (sử dụng Awaitility hoặc polling DB).
  4. Assert:
     - `status` của order cập nhật sang `FAILED` hoặc `CANCELLED` (tuỳ implement hiện tại).
     - Nếu có trường `failureReason` → được set message tương ứng.

### 4.4.2. Compensation Idempotency — Duplicate Failure Event

- **Mục tiêu**: Nhận lại cùng một failure event không làm thay đổi trạng thái order thêm lần nữa.
- **Steps**:
  1. Seed order `O001` với `status=FAILED` (đã rollback trước đó).
  2. Publish lại `InventoryReservationFailedEvent` với `orderId=O001`.
  3. Assert:
     - `status` vẫn giữ nguyên `FAILED`.
     - Không có exception.
     - Log có message dạng `"Duplicate compensation event ignored"` (nếu project đã log).

---

## 4.5. End‑to‑End Saga Flow (E2E — Later)

Các test E2E dưới đây sẽ được thực hiện khi hạ tầng Testcontainers hoàn chỉnh hơn (Kafka, PostgreSQL, MongoDB):

- [x] **4.6.1 — Happy Path**: Tạo order → PENDING → `ORDER_CREATED` → inventory confirm → `INVENTORY_CONFIRMED` → order CONFIRMED.
- [x] **4.6.2 — Insufficient Inventory**: `ORDER_CREATED` → `INVENTORY_FAILED` → order FAILED/CANCELLED.
- [ ] **4.6.3 — Cancel Flow**: Sau khi order CONFIRMED, gửi cancel → roll back kho (sẽ cần thêm stock reservation).
- [ ] **4.6.4 — Timeout / Service Down**: Inventory Service down → order ở PENDING hoặc timeout theo policy retry.

> Ghi chú: Kế hoạch mức Service (Component E2E) bằng Testcontainers đã hoàn thiện cho Happy Path và Rollback. Các kịch bản Cancel Flow và Timeout sẽ được thực hiện sau tính năng App được implement đầy đủ.

---

## 4.6. True End-to-End Testing (Docker Compose + Jest) - [PENDING]

Để test toàn trình luồng Saga đi qua cả Spring Boot (Order Service) và NestJS (Inventory Service), chúng ta sẽ sử dụng phương án Black-box Testing với Docker Compose.

**Kế hoạch triển khai (Khi có yêu cầu):**

1. **Khởi tạo thư mục `e2e-tests`**:
   - Khởi tạo Node.js project (`package.json`) dùng **Jest** và **Axios** (Nằm ngoài thư mục `services`).
2. **Cấu hình `docker-compose.e2e.yml`**:
   - Dựng chung hạ tầng Kafka, Postgres, Mongo.
   - Build và chạy cả Docker image của Order Service lẫn Inventory Service.
   - Expose port của API Gateway hoặc Order Service cho Jest gọi tới.
3. **Viết kịch bản test Polling**:
   - *Happy Path*: POST API tạo đơn với `productId="P001"`. Dùng vòng lặp gọi GET API liên tục đến khi `status == CONFIRMED`.
   - *Rollback Path*: POST API tạo đơn với `productId="OUT_OF_STOCK"`. Gọi GET liên tục đến khi `status == CANCELLED`.
4. **Viết Bash Script chạy tự động**:
   - `docker-compose up -d` -> Chờ healthcheck -> Chạy `npm run test` -> `docker-compose down`.
