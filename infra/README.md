# Infrastructure (Backing Services)

Thư mục này chứa toàn bộ cấu trúc và cấu hình hệ thống máy chủ tiện ích (Backing Services) nền tảng của dự án **Nexus Microservices Lab**.

Mọi thiết lập ở đây phụ thuộc vào file `docker-compose.yml`, cho phép bạn khởi chạy toàn bộ Hệ quản trị độ bộ hạ tầng chỉ với 1 dòng lệnh mà không phải cài đặt trực tiếp lẻ tẻ trên máy chủ thật.

---

## 🛠️ Danh sách các Services

| Tên Service | Tiện ích (Role) | Cổng Host (Port) | Mật khẩu truy cập | Usage/Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **PostgreSQL 15** | Relational Database (SQL) | `5432` | `nexus_user` / `nexus_password` | Lưu trữ dữ liệu chuẩn ACID cho Order/User Service. Bật `wal_level=logical` cho CDC. |
| **MongoDB 6.0** | NoSQL Database | `27017` | `root` / `rootpassword` | Lưu trữ cấu trúc JSON linh hoạt cho Product Catalog, Inventory. |
| **Redis 7** | Caching, Rate Limit, Session | `6379` | *None* | Cắm mốc (Tăng tốc đọc DB, Limit lưu lượng API Gateway). |
| **RabbitMQ 3** | Message Broker (Sync/Async) | `5672` (AMQP) <br> `15672` (UI) | `admin` / `admin123` | Quản lý Hàng đợi (Message Queue) cho Task xử lý chậm. |
| **Kafka 3.7.0** | Event Streaming (Pub/Sub) | `9092` | *None* | Luồng sự kiện thời gian thực (Event-Driven), Saga Pattern Choreography. |
| **Elasticsearch 9.2.0** | Search Engine (Fast Read) | `9200` | *None* | Database phục vụ truy vấn tốc độ cao (CQRS). Hỗ trợ ES|QL mạnh mẽ. |
| **Kibana 8.12.2** | Data Visualization (ES UI) | `5601` | *None* | Giao diện quản lý và truy vấn Elasticsearch. |
| **Kafka Connect** | CDC Pipeline Engine | `8083` | *None* | Nền tảng chạy các Connector (Debezium Postgres, ES Sink) để đồng bộ dữ liệu tự động. |
| **Keycloak 24** | Identity & Access Management | `8081` | `admin` / `admin` | Trung tâm bảo mật (OAuth2/OpenID). Cấp phát JWT Access Token, SSO. |

---

## 🏗️ Kiến trúc Tự động hóa CDC (Change Data Capture)

Hệ thống hạ tầng Nexus hỗ trợ cơ chế đồng bộ dữ liệu **Tự động 100%** từ Postgres sang Elasticsearch thông qua mô hình CDC:

1.  **Cấu trúc thư mục chuyên nghiệp:**
    *   `connectors/`: Chứa các file cấu hình nghiệp vụ dạng JSON (Postgres Source & ES Sink).
    *   `scripts/`: Chứa các kịch bản Bash cài đặt plugin (`kafka-connect-init.sh`) và tự động kích hoạt Connector (`cdc-provisioning.sh`).
2.  **Cơ chế vận hành:**
    *   Sử dụng **Advanced `depends_on`** trong Docker Compose để đảm bảo các dịch vụ hạ tầng lên đúng thứ tự.
    *   Service `cdc-provisioner` sẽ tự động đợi `kafka-connect` sẵn sàng (`service_healthy`) rồi mới tiến hành nạp cấu hình.

---

## 🚀 Hướng dẫn vận hành Lab (Lab Manager)

Để tự động hóa và đơn giản hóa việc quản lý hơn 10 dịch vụ hạ tầng, chúng tôi đã xây dựng bộ công cụ **Lab Manager**. Bạn không cần nhớ các lệnh Docker phức tạp nữa.

### 1. Sử dụng trên Windows (PowerShell)
Mở Terminal tại thư mục `infra/` và sử dụng lệnh `.\manage.ps1`:
- **Khởi động toàn bộ:** `.\manage.ps1 start`
- **Dừng hệ thống:** `.\manage.ps1 stop`
- **Kiểm tra trạng thái:** `.\manage.ps1 status`
- **Xem Log dịch vụ:** `.\manage.ps1 logs <tên_service>` (VD: `.\manage.ps1 logs kafka-connect`)
- **Dọn dẹp & Reset dữ liệu project:** `.\manage.ps1 clean`
- **Dọn dẹp Volume rác hệ thống:** `.\manage.ps1 prune`

### 2. Sử dụng trên Linux/WSL (Bash)
Cấp quyền thực thi lần đầu: `chmod +x manage.sh`, sau đó sử dụng:
- `./manage.sh {start|stop|status|logs|clean|prune}`

---

## 🛠️ Lưu ý về Tài nguyên (Optimization)
Hệ thống đã được cấu hình **Resource Limits** (Giới hạn RAM) cho từng Container để đảm bảo Lab có thể chạy mượt mà trên máy cá nhân có RAM từ 16GB trở lên. 

Nếu máy bạn bị giật lag, hãy dùng lệnh `status` để kiểm tra container nào đang chiếm dụng nhiều tài nguyên nhất.
