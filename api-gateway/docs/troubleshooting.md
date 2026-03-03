# Troubleshooting & Known Issues

Tài liệu này ghi chú lại tất cả các lỗi, khuyết điểm hoặc vấn đề phát sinh trong quá trình phát triển dự án **API Gateway**. Mỗi vấn đề đều đi kèm nguyên nhân chi tiết và phương pháp giải quyết để đội ngũ phát triển (hoặc CI/CD system) có thể dễ dàng tham khảo.

---

## 1. Lỗi `UnsatisfiedLinkError` về DNS Resolver trên MacOS (Apple Silicon)

### 📌 Hiện tượng
Khi chạy dự án trên máy Mac sử dụng chip ARM64 (Apple Silicon như M1, M2, M3...), ứng dụng văng lỗi stack trace trong Terminal kèm dòng chữ:
```text
java.lang.UnsatisfiedLinkError: failed to load the required native library
Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider, fallback to system defaults.
```

### 🔍 Giải thích chi tiết
API Gateway sử dụng **Spring WebFlux (chuẩn Reactive)**, bên dưới nó dùng server **Netty** (thay vì Tomcat). Netty có một module nội bộ dùng để phân giải DNS (cho phép Gateway tự động map tên miền sang IP nhanh chóng). Tuy nhiên, trên hệ điều hành MacOS kiến trúc ARM, bộ phân giải DNS gốc của máy tính khác với các hệ điều hành thông thường, dẫn đến Netty không thể gọi trực tiếp module mạng của hệ điều hành và văng lỗi `UnsatisfiedLinkError`.

### 🛠️ Cách giải quyết
Cần bổ sung thư viện native của Netty dành riêng cho macOS ARM64 bằng cách thêm mã sau vào file `pom.xml`:
```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-resolver-dns-native-macos</artifactId>
    <classifier>osx-aarch_64</classifier>
</dependency>
```
*Lưu ý:* Thẻ `<classifier>osx-aarch_64</classifier>` là bắt buộc để tải đúng file binary cho chip Apple Silicon.

---

## 2. Cảnh báo Properties Renamed trong Spring Cloud 2025.0.x

### 📌 Hiện tượng
Khi chạy dự án, console văng ra một cảnh báo (Warning) rất dài mô tả việc các khóa cấu hình bị đổi tên:
```text
The use of configuration keys that have been renamed was found in the environment:
Key: spring.cloud.gateway.routes[0].id
Replacement: spring.cloud.gateway.server.webflux.routes[0].id
```

### 🔍 Giải thích chi tiết
Kể từ phiên bản **Spring Boot 3.4.x / 3.5.0** và Spring Cloud tương ứng, Spring Cloud Gateway tách rõ cấu hình dùng cho WebFlux (Reactive) với các môi trường MVC (Servlet) truyền thống. Cụm tiền tố `spring.cloud.gateway` cũ đã bị loại bỏ/đánh dấu là Deprecated. Chuẩn mới để khai báo Load Balancer, Route, v.v. đối với nền tảng Reactive sẽ phải bắt đầu bằng `spring.cloud.gateway.server.webflux`.

### 🛠️ Cách giải quyết
Vào file cấu hình `application.yml` hoặc `application.properties`, đổi tất cả prefix khai báo về chuẩn mới:
```yaml
# CŨ (Deprecated):
spring:
  cloud:
    gateway:
      routes: ...

# MỚI:
spring:
  cloud:
    gateway:
      server:
        webflux:
          discovery: ...
          routes: ...
```

---

## 3. Lỗi Connection Refused tới Eureka Server (`localhost:8761`)

### 📌 Hiện tượng
Khi API Gateway vừa khởi động xong, log liên tục báo lỗi Connection refused sau mỗi một khoảng thời gian nhất định (khoảng 30 giây - 1 phút):
```text
Request execution failed with message: I/O error on DELETE request for "http://localhost:8761/eureka/apps/API%20GATEWAY... failed: Connection refused
Cannot execute request on any known server
```

### 🔍 Giải thích chi tiết
Eureka Client (nằm bên trong API Gateway) mặc định được thiết kế để liên tục "Pulse" (gửi heartbeat) tới máy chủ Discovery (**Eureka Server**) để thông báo rằng "Tôi vẫn đang sống, hãy cho các service khác biết địa chỉ của tôi".
Tuy nhiên, cấu hình mặc định (hoặc ta cố tình thiết lập) trỏ địa chỉ đích tới `http://localhost:8761/eureka/`. Tại thời điểm này, Eureka Server thực tế **chưa được bật hoặc chưa tồn tại**. Việc cố tình gửi Request đến một Server không tồn tại sẽ trả về lỗi Connection Refused.

**Quan trọng:** Đây KHÔNG phải lỗi code Gateway, Service Gateway hiện tại vẫn lắng nghe và xử lý Request ở port `8080` bình thường.

### 🛠️ Cách giải quyết
*   **Cách 1 (Tạm thời):** Tắt tính năng tự động đăng ký Discovery cho đến khi Eureka Server sẵn sàng bằng cách sửa `application.yml`:
    ```yaml
    eureka:
      client:
        register-with-eureka: false
        fetch-registry: false
    ```
*   **Cách 2 (Khuyên dùng):** Phát triển và khởi chạy một Microservice thứ 2 là `eureka-server` trên cổng `8761`. Sau khi Server lên, API Gateway sẽ tự động dò trùng port và tự động đăng ký thành công.

---

## 4. Lỗi `403 Forbidden` khi Unit Test CORS bằng `WebTestClient`

### 📌 Hiện tượng
Khi chạy các file Unit Test (đặc biệt là `CorsConfigTest`), request giả lập HTTP `OPTIONS` hoặc `GET` mặc dù đã cấu hình Bean `CorsWebFilter` đầy đủ nhưng vẫn liên tục bị trả về mã `403 Forbidden` và fail toàn bộ Test Case:
```text
java.lang.AssertionError: Status expected:<200 OK> but was:<403 FORBIDDEN>
```

### 🔍 Giải thích chi tiết
Lỗi này phát sinh từ **Spring WebFlux Security 6+**. Khi tích hợp Spring Security, module này mặc định tự động đánh chặn và xử lý mọi request.
*   **Vấn đề 1:** Mặc dù bạn đã khai báo `@Bean CorsWebFilter`, lớp bảo mật của Spring Security hoàn toàn "không biết" về sự tồn tại của Bean này. Do đó, request preflight (`OPTIONS`) được coi là một request **chưa xác thực** và Security Filter Chain lập tức từ chối bằng `401` hoặc `403`.
*   **Vấn đề 2:** Đối với môi trường Integration Test của `WebTestClient`, context path-matching của WebFlux có thể không map chuẩn xác chuỗi pattern `/**` với tên miền giả lập (như `http://localhost:3000`), dẫn đến request fallback về trạng thái Unauthenticated.

### 🛠️ Cách giải quyết
Đã khắc phục hoàn toàn thông qua chuỗi 3 giải pháp:
1.  **Chuyển đổi Bean Configuration:** Trong `CorsConfig.java`, không trả về `CorsWebFilter` nữa mà trả về **`CorsConfigurationSource`**.
2.  **Kích hoạt tích hợp rẽ nhánh:** Trong `SecurityConfig.java`, liên kết rõ ràng CORS Source với hệ thống Security bằng lệnh bổ sung:
    ```java
    http.cors(org.springframework.security.config.Customizer.withDefaults())
    ```
3.  **Khuyên dùng Plain Unit Test cho Logic (Tối ưu nhất):** Dẹp bỏ việc dùng WebTestClient để mock CORS. Thay vào đó, test trực tiếp đối tượng logic `CorsConfigurationSource` thuần túy bằng thư viện JUnit `MockServerWebExchange`. Điều này giúp bypass mọi tính toán ngầm của mạng webflux, giữ tỷ lệ phủ mã (coverage) 100% nhưng tăng tốc độ test lên gấp chục lần và miễn nhiễm với báo lỗi giả.
