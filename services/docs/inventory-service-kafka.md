# Inventory Service — Kafka Consumer & Saga Participant

Chi tiết cách Inventory Service (NestJS) lắng nghe, xử lý và phản hồi trong chuỗi Saga Choreography.

---

## 🔄 Luồng xử lý khi nhận Event từ Kafka

```mermaid
sequenceDiagram
    participant Kafka as Kafka Broker
    participant Controller as InventoryController
    participant Service as InventoryService
    participant MongoDB as MongoDB

    Kafka->>Controller: Consume "ORDER_CREATED" từ order-events-topic
    Controller->>Controller: Kiểm tra eventType == ORDER_CREATED?
    Controller->>Service: deductStock(productId, quantity)
    Service->>MongoDB: findOne({ productId })

    alt Không tìm thấy sản phẩm
        MongoDB-->>Service: null
        Service-->>Controller: { success: false, message: "Không tồn tại" }
        Controller->>Kafka: Emit "INVENTORY_FAILED" → inventory-events-topic
    else Tồn kho < quantity (Hết hàng)
        MongoDB-->>Service: product (quantity không đủ)
        Service-->>Controller: { success: false, message: "Hết hàng" }
        Controller->>Kafka: Emit "INVENTORY_FAILED" → inventory-events-topic
    else Đủ hàng
        MongoDB-->>Service: product (quantity đủ)
        Service->>MongoDB: product.quantity -= quantity → save()
        Service-->>Controller: { success: true, message: "Đã trừ kho" }
        Controller->>Kafka: Emit "INVENTORY_CONFIRMED" → inventory-events-topic
    end
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
- **`@EventPattern('order-events-topic')`**: Decorator gắn handler vào Kafka topic.
- Nhận `OrderEvent`, gọi `deductStock()`, đóng gói phản hồi, `emit()` vào `inventory-events-topic`.

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
