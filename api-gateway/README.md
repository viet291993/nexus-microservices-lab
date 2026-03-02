# API Gateway: Nexus Microservices

*The central nervous system and single entry point of the Nexus Architecture.*

---

## 🏗️ Architecture Role

* **Dynamic Routing:** Điều phối requests tới các backend microservices thông qua Spring Cloud Gateway.
* **Service Discovery:** Tích hợp Eureka Client để tự động nhận diện IP, Port của các service ẩn phía sau.
* **Security & Authentication:** Điểm chốt chặn duy nhất (Global Filter) để xác thực JWT token.
* **Resilience:** Fallback & Circuit Breaker với Resilience4j bảo vệ toàn cục hệ thống khi các service con sụp đổ.
* **Rate Limiting:** Sử dụng Redis Token Bucket chặn spam request, bảo vệ băng thông cho từng Client/IP.

## 🛠️ Tech Stack

* **Core Framework:** Spring Boot 3.5.0, WebFlux (Reactive Programming).
* **Gateway Layer:** Spring Cloud Gateway (Netty Server).
* **Discovery:** Spring Cloud Netflix Eureka Client.
* **Data & Cache:** Spring Data Reactive Redis.
* **Observability:** Spring Boot Actuator.

---

## 🧠 Implementation Notes (Key Learnings)

### 1. Tại sao dùng WebFlux thay vì WebMVC?
Gateway chịu trách nhiệm chặn và chuyển tiếp hàng ngàn requests mỗi giây. Sử dụng **Spring WebFlux** với mô hình Non-blocking I/O (qua Netty Server) giúp hệ thống không phải giữ Thread chờ đợi trong quá trình gọi sang service khác, giúp tối đa hóa thông lượng (throughput) mà tiêu tốn cực ít RAM so với Tomcat truyền thống.

### 2. Rate Limiting bằng Redis
Sử dụng thuật toán Token Bucket được cấu hình qua `RedisRateLimiter` gốc của Spring Cloud. Khi có traffic spike, Gateway có thể tính toán Request/giây trên bộ nhớ đệm tốc độ cao của Redis, nếu vượt ngưỡng nó sẽ ném về `429 Too Many Requests` ngay tức thì để bảo vệ Database phía sau.

---

## 🚦 Status & Roadmap

- [x] **Project Initialization:** Setup cấu trúc Spring Boot 3.5 với Java 17, tích hợp Netty Server & WebFlux.
- [x] **Basic Routing & Properties:** Map cấu hình Yaml với pattern nhận diện Loadbalancer (`lb://`). 
- [ ] **Eureka Discovery Config:** Liên kết và lắng nghe địa chỉ từ Eureka Server (tại `localhost:8761`).
- [ ] **Global Auth Filter:** Xây dựng bean Custom Filter để bắt JWT Header.
- [ ] **Redis Rate Limiting:** Kích hoạt tính năng chặn Request Spam theo IP.
- [ ] **CORS Policy:** Cấu hình Global CORS cho toàn bộ các web app trỏ tới.

---

## 🚀 How to Run

### Yêu cầu hệ thống (Prerequisites)
* Java 17+
* Redis Server 
* Eureka Server (Đang chạy, ví dụ HTTP `localhost:8761`)

### Lệnh khởi chạy nhanh
```bash
# Xóa thư mục cũ và đóng gói ứng dụng
./mvnw clean package -DskipTests

# Khởi chạy API Gateway
./mvnw spring-boot:run
```

---

## 🛠️ Troubleshooting & Known Issues

Trong quá trình phát triển (đặc biệt trên môi trường macOS ARM64), nếu bạn gặp các lỗi khởi chạy như `UnsatisfiedLinkError` về DNS Resolver, `Properties Renamed` warning, hay lỗi `Connection refused` đến máy chủ Eureka, vui lòng xem tài liệu gỡ lỗi chi tiết tại:

👉 **[docs/troubleshooting.md](./docs/troubleshooting.md)**
