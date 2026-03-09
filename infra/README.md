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
| **Elasticsearch 9.2.0** | Search Engine (Fast Read) | `9200` | *None* | Database phục vụ truy vấn tốc độ cao (CQRS). Hỗ trợ `ES|QL` mạnh mẽ. |
| **Kibana 9.2.0** | Data Visualization (ES UI) | `5601` | *None* | Giao diện quản lý và truy vấn Elasticsearch. |
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


## 🚀 Phase 5: CI/CD & Docker Swarm Deployment

Hệ thống đã hỗ trợ quy trình triển khai tự động (Continuous Deployment) và chạy trên Cluster thông qua Docker Swarm.

### 1. GitHub Actions (CI/CD)
Workflow `cd.yml` tự động hóa việc Build & Push Docker Images cho toàn bộ 6 services lên **GitHub Container Registry (GHCR)**.
- **Kích hoạt:** Tự động khi push vào nhánh `main` hoặc tạo Git Tag (VD: `v1.0.0`).
- **Bảo mật:** Sử dụng **Distroless Images** và Pin GitHub Actions bằng commit SHA để đảm bảo an toàn tối đa.
- **Trạng thái:** Hiện tại hỗ trợ tự động Build & Push. Bước Deploy lên Swarm đang ở dạng **Placeholder** và yêu cầu cấu hình SSH Secrets (`SWARM_HOST`, `SWARM_SSH_KEY`) để chạy tự động hoàn toàn.

### 2. Triển khai Docker Swarm (Cluster)
Sử dụng file `docker-stack.yml` để chạy dự án trên môi trường Cluster thực tế.

**Các bước triển khai thủ công:**
```bash
# 1. Khởi tạo Swarm (nếu chưa có)
docker swarm init

# 2. Triển khai Stack (Yêu cầu đăng nhập GHCR)
export GITHUB_REPOSITORY_OWNER=your_username
export IMAGE_TAG=latest
docker stack deploy -c docker-stack.yml nexus-stack
```

**Kích hoạt Tự động hóa Deployment (Full CD):**
Để bật tính năng tự động deploy khi merge vào `main`, bạn cần:
1. Tạo **GitHub Environment** tên là `production-swarm`.
2. Cấu hình **Environment Protection Rule** (ví dụ: cần có người duyệt) để kiểm soát việc rollout.
3. Thêm các Secrets: `SWARM_HOST`, `SWARM_USER`, và `SWARM_SSH_KEY` vào Repo.
4. Cập nhật bước `Deploy to Docker Swarm via SSH` trong `cd.yml` để sử dụng các thông tin này.

**Lợi ích của Docker Swarm:**
- **High Availability:** Tự động khôi phục container bị lỗi (`restart_policy`).
- **Scaling:** Dễ dàng tăng số lượng bản sao (VD: `api-gateway` mặc định chạy 2 replicas).
- **Overlay Network:** Giao tiếp bảo mật giữa các Service trên nhiều Node.

---

## 🛠️ Lưu ý về Tài nguyên (Optimization)
Hệ thống đã được cấu hình **Resource Limits** (Giới hạn RAM) cho từng Container để đảm bảo Lab có thể chạy mượt mà trên máy cá nhân có RAM từ 16GB trở lên.

Nếu máy bạn bị giật lag, hãy dùng lệnh `status` để kiểm tra container nào đang chiếm dụng nhiều tài nguyên nhất.
