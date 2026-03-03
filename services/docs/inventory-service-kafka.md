# Inventory Service — Kafka Consumer & Saga Participant

Chi tiết cách Inventory Service (NestJS) lắng nghe, xử lý và phản hồi trong chuỗi Saga Choreography.

---

## 🔄 Luồng xử lý khi nhận Event từ Kafka

```mermaid
flowchart TD
    A["📩 Nhận event ORDER_CREATED từ topic saga-orders-topic"] --> B{eventType == ORDER_CREATED?}
    B -- Không --> C["⚠️ Bỏ qua event"]
    B -- Có --> D["📦 Gọi InventoryService.deductStock()"]
    D --> E["🔍 Tìm sản phẩm trong MongoDB theo productId"]
    E --> F{Tìm thấy?}
    F -- Không --> G["❌ Trả INVENTORY_FAILED: Sản phẩm không tồn tại"]
    F -- Có --> H{quantity >= orderQuantity?}
    H -- Không --> I["❌ Trả INVENTORY_FAILED: Hết hàng"]
    H -- Có --> J["✅ Trừ kho, lưu MongoDB"]
    J --> K["✅ Trả INVENTORY_CONFIRMED"]
    G --> L["📤 Emit phản hồi vào topic saga-inventory-response"]
    I --> L
    K --> L
```

---

## 🏗️ Kiến trúc Hybrid Application (HTTP + Kafka)

NestJS hỗ trợ chạy đồng thời nhiều Transport. Trong `main.ts`:

1. **`NestFactory.create(AppModule)`**: Tạo ứng dụng HTTP Express bình thường (port 8082).
2. **`app.connectMicroservice({ transport: Transport.KAFKA })`**: Gắn thêm "tai nghe" Kafka.
3. **`app.startAllMicroservices()`**: Bật Kafka Consumer trước.
4. **`app.listen(port)`**: Mở cổng HTTP sau.

=> Inventory Service vừa phục vụ REST API (health check), vừa xử lý Kafka event song song.

---

## ⚙️ Các file chính

### `inventory.schema.ts`
- Mongoose Schema định nghĩa document MongoDB: `productId` (unique), `name`, `quantity`.
- `timestamps: true` tự động gắn `createdAt`/`updatedAt`.

### `inventory.service.ts`
- **`deductStock(productId, quantity)`**: Tìm sản phẩm → kiểm tra tồn kho → trừ kho → lưu MongoDB.
- Trả `{ success, message }` cho Controller quyết định gửi event phản hồi nào.

### `inventory.controller.ts`
- **`@EventPattern('saga-orders-topic')`**: Decorator gắn handler vào Kafka topic.
- Nhận `OrderEvent`, gọi `deductStock()`, đóng gói phản hồi, `emit()` vào `saga-inventory-response`.

### `inventory.module.ts`
- Đăng ký `MongooseModule.forFeature` (Schema kho).
- Đăng ký `ClientsModule` (Kafka Producer) với token `'KAFKA_SERVICE'` để Controller inject.

---

## 🔑 Biến môi trường (.env)

| Biến              | Mặc định                          | Mô tả                                  |
| :---------------- | :-------------------------------- | :-------------------------------------- |
| `PORT`            | `8082`                            | Cổng HTTP của Inventory Service.        |
| `MONGODB_URI`     | `mongodb://root:rootpassword@...` | Connection string MongoDB.              |
| `KAFKA_BROKERS`   | `localhost:9092`                  | Danh sách Kafka Broker (phân cách `,`). |
| `KAFKA_CLIENT_ID` | `inventory-service-client`        | ID định danh Kafka Client.              |
| `KAFKA_GROUP_ID`  | `inventory-service-group`         | Consumer Group ID.                      |
