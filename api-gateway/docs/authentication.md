# Phân tích luồng Global Authentication (OAuth2/OIDC với Keycloak)

Tài liệu giải thích cách cài đặt và hoạt động của kiến trúc bảo mật (Security Layer) bên trong hệ thống **Nexus API Gateway**. Theo mô hình này, chúng ta sử dụng **Keycloak** đóng vai trò là một IAM (Identity and Access Management) thay vì tự code thủ công dịch vụ Auth.

---

## 🏗️ 1. Tại sao dùng Keycloak theo chuẩn OAuth2 Resource Server?

Trong cấu trúc Microservice phân tán, nếu tự code (custom filter + JWT Utilities):
* **Bảo mật lỏng lẻo:** Sẽ rất khó và mất thời gian để code ra các tính năng Refresh Token, xoay (rotate) Signing Keys định kỳ để phòng chống hack, hay tích hợp bảo mật hai lớp.
* **Tải CPU lãng phí:** Gây quá tải việc tính mật mã băm khi dùng chung 1 khóa Secret cổ điển.

**Giải pháp API Gateway làm Resource Server:** 
Gateway đóng vai trò là "cáp quang kiểm duyệt". Mọi API gửi từ Client (ví dụ app Mobile, Web) bắt buộc phải dán theo `Access Token` xin từ hệ thống Keycloak.
Gateway sẽ tự biết cách lấy **Public Key** một cách công khai từ địa chỉ của Keycloak để giải mã (Verify) cái token đó. Nó tự động hóa 100% nhờ thư viện `spring-boot-starter-oauth2-resource-server`. Nếu sai hoặc hết hạn, Gateway **trả ngay mã lỗi chuẩn HTTP 401 Unauthorized**.

---

## 🛠️ 2. Các thành phần cốt lõi của Filter

### A. Công lý giải mã - `spring.security.oauth2`
Không còn class `JwtUtil.java` tốn kém. Dòng cấu hình duy nhất bạn cần là địa chỉ `issuer-uri` trong file `application.yml` trỏ về Realm của Keycloak:
`issuer-uri: http://localhost:8081/realms/master`

Spring Security sẽ âm thầm gọi qua cổng 8081 mở sẵn trên máy, trích xuất cấu hình bảo mật tiêu chuẩn (JWK Set) để lấy các chìa khóa công khai (Public Key). Nó sẽ dùng Public key này đối chiếu chữ ký (Signature) bên trong Token người dùng đưa lên.

### B. Vùng miễn trừ - `SecurityConfig.java`
Để thay thế cho vòng lọc RouteValidator tự viết tay, chúng ta dùng class `SecurityConfig`. 
Dùng `pathMatchers("/eureka/**", "/actuator/**", "/fallback/**").permitAll()` cho phép những cổng Monitor, ngắt mạch, và Service Discovery được qua lại thoải mái không cần xuất trình Token.
Quy tắc là: `anyExchange().authenticated()` -> Toàn bộ các luồng còn lại bị chặn đứng nếu không có Access Token hợp lệ.

---

## 🚦 3. Lưu ý quá trình Integration Testing
Bạn sẽ cần phải mở Keycloak Admin Console tại cổng `8081`, tự tạo một User mẫu, xin cấp Client Secret hoặc Token bằng lệnh cURL, và dùng Postman kẹp cái Token đó vào mục `Authorization: Bearer <token>` để kiểm chứng việc Gateway sẽ ném qua 1 luồng Route (Ví dụ vào User Service).

