# Infrastructure (Backing Services)

Thư mục này chứa toàn bộ cấu trúc và cấu hình hệ thống máy chủ tiện ích (Backing Services) nền tảng của dự án **Nexus Microservices Lab**.

Mọi thiết lập ở đây phụ thuộc vào file `docker-compose.yml`, cho phép bạn khởi chạy toàn bộ Hệ quản trị độ bộ hạ tầng chỉ với 1 dòng lệnh mà không phải cài đặt trực tiếp lẻ tẻ trên máy chủ thật.

---

## 🛠️ Danh sách các Services

| Tên Service | Tiện ích (Role) | Cổng Host (Port) | Mật khẩu truy cập | Usage/Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **PostgreSQL 15** | Relational Database (SQL) | `5432` | `nexus_user` / `nexus_password` | Lưu trữ dữ liệu chuẩn ACID cho Order/User Service. |
| **MongoDB 6.0** | NoSQL Database | `27017` | `root` / `rootpassword` | Lưu trữ cấu trúc JSON linh hoạt cho Product Catalog, Inventory. |
| **Redis 7** | Caching, Rate Limit, Session | `6379` | *None* | Cắm mốc (Tăng tốc đọc DB, Limit lưu lượng API Gateway). |
| **RabbitMQ 3** | Message Broker (Sync/Async) | `5672` (AMQP) <br> `15672` (UI) | `admin` / `admin123` | Quản lý Hàng đợi (Message Queue) cho Task xử lý chậm. Mở cổng 15672 trên Browser để xem Dashboard. |
| **Kafka 3.5** | Event Streaming (Pub/Sub) | `9092` | *None* | Luồng sự kiện thời gian thực (Event-Driven), Saga Pattern Choreography. Chạy Mode KRaft (bỏ Zk). |
| **Keycloak 24** | Identity & Access Management | `8081` | `admin` / `admin` | Trung tâm bảo mật (OAuth2/OpenID). Cấp phát JWT Access Token, SSO (Mở `localhost:8081`). |

---

## 🚀 Hướng dẫn khởi chạy (Usage)

Vui lòng chắc chắn rằng bạn đã mở **Docker Desktop** (hoặc tiến trình Docker Daemon) trên máy chủ trước khi thực thi lệnh.

### 1. Khởi chạy toàn bộ hệ thống
Gõ lệnh sau tại thiết bị chính:
```bash
docker-compose up -d
```
Docker sẽ lấy config, tải images nặng từ Internet (nếu chạy lần đầu), giải nén và kích hoạt dưới dạng container ẩn.

### 2. Khởi chạy từng phần (Chống nghẽn RAM/Mạng)
Nếu bạn chỉ tham gia lập trình/test cho 1 Service nhất định (Gateway) và máy bạn bị thiếu RAM, bạn nên chạy riêng biệt tên dịch vụ bằng lệnh:

```bash
# Ví dụ: Chỉ chạy Redis và Keycloak để kiểm thử Gateway (Tiết kiệm >2GB RAM)
docker-compose up -d redis keycloak
```

### 3. Tắt và dọn dẹp (Teardown)
Mọi dữ liệu của hệ thống DB/Keycloak đã được lưu vào phân vùng `volumes` riêng biệt cứng (Ví dụ `postgres_data`, `redis_data`). Khởi động lại hệ thống không làm mất dữ liệu.

Tắt toàn bộ container nhưng giữ lại Data:
```bash
docker-compose down
```

👉 *Cảnh báo siêu nguy hiểm:* Lệnh sau sẽ **hủy diệt TOÀN BỘ dữ liệu cấu hình thực tế** của Database nội tại. Chỉ sử dụng nếu bạn muốn reset dự án từ con số 0:
```bash
docker-compose down -v
```
