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
│   ├── app.module.ts                    # Module gốc: đăng ký Config, MongoDB, InventoryModule, EurekaModule, HealthModule.
│   ├── inventory/
│   │   ├── inventory.module.ts          # Đăng ký Schema, Kafka Client, Controller, Service.
│   │   ├── inventory.controller.ts      # Kafka Consumer: nhận ORDER_CREATED, gửi phản hồi.
│   │   └── inventory.service.ts         # Logic trừ kho MongoDB.
│   ├── infra/
│   │   └── health/
│   │       ├── health.controller.ts     # REST endpoint GET /health cho health‑check (Eureka, K8s...).
│   │       └── health.module.ts         # Gom nhóm hạ tầng health vào một module riêng.
│   ├── eureka/
│   │   ├── eureka.service.ts            # Đăng ký/deregister INVENTORY-SERVICE với Eureka bằng lifecycle của Nest.
│   │   └── eureka.module.ts             # Module bọc EurekaService, import vào AppModule.
│   └── shared/
│       └── events/models/               # Thư mục chứa DTOs được sinh tự động.
├── .env                                 # Biến môi trường thật dùng khi chạy local.
├── .env.example                         # Mẫu biến môi trường (không chứa mật khẩu thật).
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
3. **AsyncAPI CLI v6**: Dự án sử dụng `@asyncapi/cli` bản mới nhất (>= 6.x) để tận dụng các bản vá bảo mật và loại bỏ các library deprecated (như `request`).
   - _Lưu ý: Yêu cầu Node.js v24+ cho CLI v6._
4. **Mệnh lệnh sinh code**:
   ```bash
   npm run generate
   ```
   _Lệnh này sẽ tự động chạy trước khi build (`prebuild`)._

---

## 🚀 Cách chạy

```bash
# Đảm bảo MongoDB (27017) và Kafka (9092) đang chạy (docker-compose).
cd services/inventory-service
npm run start:dev
```

> Tip: copy `.env.example` thành `.env` rồi sửa lại `MONGODB_URI` (user/pass/host) cho từng môi trường.

---

## ⚙️ Luồng khởi động kỹ thuật (Bootstrap)

- **Bước 1 — Tạo HTTP App**
  - `main.ts`:
    - `NestFactory.create(AppModule)` → khởi tạo HTTP server (Express) với toàn bộ module đã đăng ký (`AppModule`).
    - Gắn `ValidationPipe` global để:
      - Tự động transform payload Kafka thành instance class (DTO).
      - Áp dụng validation runtime theo decorator `class-validator`.

- **Bước 2 — Gắn microservice Kafka**
  - `app.connectMicroservice<MicroserviceOptions>({ transport: Transport.KAFKA, ... })`:
    - Cấu hình:
      - `client.brokers`: đọc từ `KAFKA_BROKERS` (mặc định `localhost:9092`).
      - `client.clientId`: `KAFKA_CLIENT_ID`.
      - `consumer.groupId`: `KAFKA_GROUP_ID`.
  - `await app.startAllMicroservices()`:
    - Đảm bảo Kafka consumer sẵn sàng **trước** khi mở cổng HTTP.

- **Bước 3 — Mở cổng HTTP**
  - `await app.listen(process.env.PORT || 8083)`:
    - Cổng HTTP phục vụ:
      - Endpoint `/health`.
      - (Có thể mở rộng thêm REST API cho kho nếu cần sau này).

---

## 🔍 Health‑check & Eureka

- **Health‑check**
  - Endpoint: `GET /health`
  - Được triển khai trong `infra/health/health.controller.ts` và gom vào `HealthModule`.
  - Dùng cho:
    - Eureka kiểm tra `statusPageUrl` / `healthCheckUrl`.
    - K8s / Docker / các hệ thống monitoring khác.

- **Eureka Service Discovery**
  - Logic đăng ký/deregister nằm trong `eureka/eureka.service.ts`, implement `OnModuleInit` và `OnModuleDestroy`.
  - `EurekaModule` được import vào `AppModule`, nên khi Nest boot/shutdown:
    - `onModuleInit()` → `EurekaService` tự `start()` và đăng ký `INVENTORY-SERVICE` lên Eureka.
    - `onModuleDestroy()` → `EurekaService` tự `stop()` và deregister service.
  - Cấu hình bằng biến môi trường (xem `.env` / `.env.example`):
    - `EUREKA_HOST` — host của Eureka Server (mặc định `localhost`).
    - `EUREKA_PORT` — cổng Eureka Server (mặc định `8761`).

### Tích hợp kỹ thuật Eureka (Native Fetch)

- **Native Fetch (Node.js 18+)**
  - Project đã loại bỏ hoàn toàn `eureka-js-client` (thư viện cũ dùng `request` đã deprecated).
  - Sử dụng native `fetch` để tương tác trực tiếp với Eureka REST API.
  - Tự động xử lý:
    - **Đăng ký (Register)**: `POST /eureka/apps/INVENTORY-SERVICE`.
    - **Gia hạn (Heartbeat)**: `PUT /eureka/apps/INVENTORY-SERVICE/{instanceId}` mỗi 30 giây.
    - **Tự động phục hồi**: Nếu Eureka Server trả về 404 (mất session), service sẽ tự động bắt đầu lại luồng đăng ký.
    - **Hủy đăng ký (Deregister)**: `DELETE` khi ứng dụng tắt.

- **EurekaService**
  - Được cài đặt trong `src/eureka/eureka.service.ts`.
  - Logic đăng ký và retry được cô lập, không làm ảnh hưởng đến thời gian boot-up của NestJS nhờ cơ chế `initiateRegistration` không block main thread.

---

## 📖 Tài liệu chi tiết

👉 Xem thêm tại **[services/docs/](../docs/)**
