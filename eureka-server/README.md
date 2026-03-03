# Eureka Server: Nexus Microservices

*The central registry for Service Discovery in the Nexus Architecture.*

---

## 🏗️ Architecture Role

* **Central Registry:** Đóng vai trò như một danh bạ điện thoại, tất cả các microservices (như API Gateway, User Service, Product Service) đều phải đăng ký thông tin liên lạc của mình (IP, Port) cho máy chủ Eureka này.
* **Client-side Load Balancing:** Kết hợp với API Gateway và Spring Cloud LoadBalancer để điều phối traffic phân tán thông minh mà không cần một máy chủ Nginx/HAProxy ở giữa.
* **Health Check & Heartbeat:** Cứ mỗi 30 giây, các Client sẽ gửi một "mạch đập" (heartbeat) về Eureka. Nếu một service ngừng gửi tín hiệu trong 90 giây, Eureka sẽ loại bỏ service đó khỏi danh sách để ngăn Gateway route request vào một node đã sập.

## 🛠️ Tech Stack

* **Core Framework:** Spring Boot 3.5.0.
* **Discovery Server:** Spring Cloud Netflix Eureka Server.
* **Observability:** Spring Boot Actuator.

---

## 🧠 Implementation Notes (Key Learnings)

### Tại sao cần Service Discovery?
Thay vì cấu hình cứng cáp IP và Cổng của từng microservice trong Gateway (ví dụ: `http://localhost:8081`, `http://localhost:8082`), ta dùng Eureka. Các service tự thông báo địa chỉ của bản thân khi vừa bật lên. API Gateway chỉ cần trỏ theo tên gọi bằng cú pháp `lb://USER-SERVICE`... Việc này cực kì quan trọng khi muốn tăng giảm Scale hệ thống linh hoạt trên Cloud.

### Cấu hình Standalone:
Bởi vì chính nó là một máy chủ, nó không cần phải đăng ký lại chính mình (như các thiết lập default của Spring Boot). Nên file config yêu cầu:
```yaml
register-with-eureka: false
fetch-registry: false 
```

---

## 🚦 Status & Roadmap

- [x] **Project Initialization:** Setup cấu trúc, chuẩn Java 17.
- [x] **Enable Server:** Kích hoạt tính năng với `@EnableEurekaServer`.
- [x] **Standalone Config:** Set `application.yml` theo chuẩn cấu hình độc lập.
- [ ] **Dockerization:** Sẵn sàng cho quá trình Deploy cùng `docker-compose.yml` sau này.

---

## 🚀 How to Run

Do API Gateway và các Service thường mặc định cắm vào Cổng `8761`, dự án này được gắn chết cổng tĩnh đó.

```bash
# Di chuyển vào thư mục eureka-server
cd eureka-server

# Build và chạy ứng dụng
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

Sau khi chạy thành công, có thể dùng trình duyệt truy cập:
👉 **[http://localhost:8761](http://localhost:8761)** để xem Dashboard giao diện Web của Spring Cloud Eureka. Khi chạy đồng thời cả `api-gateway`, bạn sẽ thấy dòng thư mục `API-GATEWAY` đang nằm trong danh sách đăng ký!
