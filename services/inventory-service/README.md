# Nexus: Inventory Service (NodeJS - NestJS)

Dịch vụ quản lý Kho hàng — Saga Participant lắng nghe Kafka để trừ kho khi có đơn hàng mới từ Order Service (Java).

---

## 📂 Cấu trúc mã nguồn

```text
inventory-service/
├── src/
│   ├── main.ts                          # Entry Point: Hybrid App (HTTP + Kafka Transport).
│   ├── app.module.ts                    # Module gốc: đăng ký Config, MongoDB, InventoryModule.
│   └── inventory/
│       ├── inventory.module.ts          # Đăng ký Schema, Kafka Client, Controller, Service.
│       ├── inventory.controller.ts      # Kafka Consumer: nhận ORDER_CREATED, gửi phản hồi.
│       ├── inventory.service.ts         # Logic trừ kho MongoDB.
│       └── schemas/
│           └── inventory.schema.ts      # Mongoose Schema: productId, name, quantity.
├── .env                                 # Biến môi trường (MONGODB_URI, KAFKA_BROKERS...).
└── package.json
```

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
