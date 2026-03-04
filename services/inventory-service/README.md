# Nexus: Inventory Service (NodeJS - NestJS)

Dịch vụ quản lý Kho hàng — Saga Participant lắng nghe Kafka để trừ kho khi có đơn hàng mới từ Order Service (Java).

---

## 📂 Cấu trúc mã nguồn

```text
inventory-service/
├── scripts/
│   └── generate-models.ts               # Script Modelina: Sinh DTOs tự động kèm class-validator.
├── src/
│   ├── main.ts                          # Entry Point: Hybrid App (HTTP + Kafka Transport).
│   ├── app.module.ts                    # Module gốc: đăng ký Config, MongoDB, InventoryModule.
│   ├── inventory/
│   │   ├── inventory.module.ts          # Đăng ký Schema, Kafka Client, Controller, Service.
│   │   ├── inventory.controller.ts      # Kafka Consumer: nhận ORDER_CREATED, gửi phản hồi.
│   │   └── inventory.service.ts         # Logic trừ kho MongoDB.
│   └── shared/
│       └── events/models/               # Thư mục chứa DTOs được sinh tự động.
├── .env                                 # Biến môi trường (MONGODB_URI, KAFKA_BROKERS...).
└── package.json
```

---

## 🛠 Kỹ thuật chính: Contract-First & Modelina

Dịch vụ này áp dụng triết lý **Contract-First** (Dùng schema làm gốc) để đảm bảo tính nhất quán với phía Java (Order Service):

1. **AsyncAPI**: Mọi sự kiện Kafka được định nghĩa trong file `shared/event-schemas/asyncapi.yaml`.
2. **Modelina**: Sử dụng mạnh mẽ thư viện `@asyncapi/modelina` thông qua script `generate-models.ts` để:
   - Tự động sinh TypeScript Classes.
   - Tự động tiêm các Decorators như `@IsString()`, `@IsNumber()`, `@IsOptional()` từ `class-validator`.
   - Đảm bảo **Type Safety** và **Runtime Validation** cho các message nhận từ Kafka.
3. **Mệnh lệnh sinh code**:
   ```bash
   npm run generate
   ```
   *Lệnh này sẽ tự động chạy trước khi build (`prebuild`).*

---

## 🚀 Cách chạy

```bash
# Đảm bảo MongoDB (27017) và Kafka (9092) đang chạy (docker-compose).
cd services/inventory-service
npm run start:dev
```

---

## 📖 Tài liệu chi tiết

👉 Xem thêm tại **[services/docs/](../docs/)**
