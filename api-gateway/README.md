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

## 🚀 How to Run

**Yêu cầu hệ thống:** Java 17+, Redis (cổng 6379), Eureka Server (cổng 8761).

```bash
# Đóng gói ứng dụng (bỏ qua test)
./mvnw clean package -DskipTests

# Khởi chạy API Gateway
./mvnw spring-boot:run
```

---

## 📚 Documentation

Để giữ README gọn gàng, toàn bộ chi tiết kỹ thuật, giải thích kiến trúc và hướng dẫn testing được chia nhỏ trong thư mục [`docs/`](./docs/):

* **[🧠 Implementation Notes & Mẹo Kiến Trúc](./docs/implementation-notes.md)**: Chi tiết về lý do chọn WebFlux, cơ chế Redis Rate Limiting, và Resilience4j Circuit Breaker.
* **[🧪 Testing Guide](./docs/testing.md)**: Hướng dẫn chạy test suite (7/7 Pass 100%) bao phủ Security, CORS, Fallback Controller.
* **[🛡️ Authentication](./docs/authentication.md)**: Cấu trúc Global Filter bắt JWT từ Keycloak.
* **[⚡ Circuit Breaker](./docs/circuit-breaker-resilience4j.md)**: Tham số cấu hình chống đứt gãy dây chuyền (Resilience4j).
* **[🛠️ Troubleshooting & Known Issues](./docs/troubleshooting.md)**: Cách khắc phục các lỗi hóc búa (macOS ARM64 DNS, Eureka Connection).
