# Nexus: Config Server

*Trái tim cấu hình (Centralized Configuration) của toàn bộ kiến trúc Nexus Microservices.*

---

## 🏗️ Vai Trò (What it does)

Thay vì mỗi Microservice (như API Gateway, Eureka, User Service) ôm khư khư một tệp `application.yml` cứng nhắc và phải khởi động lại server mỗi khi có thay đổi cấu hình, **Config Server** xuất hiện để giải quyết bài toán:

- **Single Source of Truth:** Nơi duy nhất chứa các biến môi trường, thông tin kết nối Database, đường dẫn Route.
- **Dynamic Reload (Hot Reloading):** Cho phép thay đổi cấu hình nóng mà không cần tắt ứng dụng qua `@RefreshScope`.
- **Security:** Quản lý tập trung các mật khẩu (ví dụ: DB Password) mà không lộ trong code repo của từng Service.

---

## 🛠️ Cấu trúc thư mục

```text
config-server/
│
├── src/main/resources/
│   ├── application.yml         <-- Cấu hình gốc của Server (Quy định port 8888 và cách đọc file)
│   └── config-repo/            <-- [MÔI TRƯỜNG NATIVE] Kho lưu trữ file yml cấp phát
│       ├── api-gateway.yml     <-- Cấu hình Route, Redis, Resilience4j cho Gateway
│       └── eureka-server.yml   <-- Cấu hình kết nối cho máy chủ Service Discovery
```

*Lưu ý: Ở môi trường Lab, chúng ta sử dụng `profile=native` để đọc cấu hình trực tiếp từ thư mục `config-repo` thao tác cho tiện. Trên thực tiễn Production, Config Server thường trỏ thẳng tới một Github Repository bảo mật chứa các mớ YAML này.*

---

## 🚀 Cách Thức Hoạt Động (How it works)

Khi một dịch vụ (VD: `api-gateway`) bắt đầu khởi động, vòng lặp sau sẽ xảy ra:
1. `api-gateway` đọc file `application.yml` nội bộ siêu nhỏ của nó. File này chỉ chứa lệnh chỉ đường: *"Đi tới http://localhost:8888 xin file cấu hình"*.
2. **Config Server** tiếp nhận yêu cầu, tìm trong `config-repo` file trùng tên (`api-gateway.yml`) rồi "quăng" ngược lại cho `api-gateway`.
3. `api-gateway` load xong đống cấu hình kéo về và mới thực sự bật server!

---

## 🔄 Refresh Cấu Hình Nóng (Actuator)

Mọi dịch vụ đã kết nối với Config Server đều được mở khoá tính năng Refresh nhờ Actuator.

Nếu bạn lỡ edit sửa file định tuyến `api-gateway.yml` trong ruột **Config Server**, thay vì phải khởi động lại Gateway cực khổ, hãy gọi lệnh API POST này để ép Gateway tự xin lại cấu hình mới:

```bash
curl -X POST http://localhost:8080/actuator/refresh
```
