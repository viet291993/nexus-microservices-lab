# Auth Service: Identity & Token Management

*Trái tim kiểm soát danh tính và cấp phép đăng nhập của hệ sinh thái Nexus Microservices.*

---

## 🏗️ Architecture Role

* **Identity Provider (Nhà cung cấp Danh tính):** Là dịch vụ duy nhất sở hữu quyền đối chiếu thông tin (Username/Password) của người dùng với cơ sở dữ liệu (PostgreSQL).
* **Token Issuer (Trạm phát Token):** Sau khi xác thực thành công, Auth Service tự tay gom các thông tin (Username, Role) thành một khối dữ liệu và băm (Hash) bằng một Key bí mật (Secret), tạo thành một **JWT Token** hoàn chỉnh trả về cho người dùng.
* **Password Encryption:** Không bao giờ lưu mật khẩu ở dạng Text thuần tự nhiên. Dịch vụ tự động giấu mật khẩu (Hashing) dưới thuật toán khó giải mã `BCrypt`.

## 🛠️ Tech Stack

* **Core Framework:** Spring Boot 3.5.0, Java 17.
* **Database & ORM:** PostgreSQL + Spring Data JPA (Hibernate).
* **Security & Tokens:** Spring Security Crypto (Băm mật khẩu) & JJWT (Build/Tạo JWT).
* **Communication:** Nằm dưới sự theo dõi của Eureka Server (Discovery). Được phân bổ Traffic bởi API Gateway ở route `/api/v1/auth/**`.

---

## 🧠 Implementation Notes (Tại sao thiết kế thế này?)

### 1. Tại sao không nhet luôn Data JPA vào API Gateway?
**Gateway** vốn là "cánh cổng mạng" được thiết kế tối giản, chạy Non-Blocking I/O để phục vụ luồng Traffic khổng lồ (Nhiệm vụ của nó là Routing & Chặn Spam/Kiểm Token). 
Lôi kết nối Database SQL (thường là luồng Blocking Database call tốn thời gian I/O đĩa cứng rất lâu) vào Gateway sẽ gây nguy hiểm sập toàn hệ thống khi lượng người đăng nhập/truy vấn DB cao. Auth Service được tách ra chạy độc lập để nếu có "Tắc nghẽn mạng do lỗi Database Authentication" thì cả ứng dụng Web vẫn còn khả năng bán hàng và các User có sẵn Token vẫn đi qua API Gateway gọi Route Product Service như thường.

### 2. Sự đồng bộ biến Mật mã Secret (JWT Core)
Dịch vụ này **tạo Token** bằng thẻ cấu hình `jwt.secret`. 
Điều bắt buộc là dòng chữ `jwt.secret` (Base64) bên trong file YAML này phải **trùng khớp 100%** với cấu hình mã `jwt.secret` bên kia Gateway (Để Gateway có thể dùng làm File chìa khóa Decode/Giải nén lại Token).

---

## 🚦 Status & Roadmap

- [x] **Project Initialization:** Setup cấu trúc Auth Service, kết nối PostgreSQL, Eureka Client.
- [ ] **Entity & Repository:** Tạo Bảng User trên DB và kho lưu trữ truy xuất thông tin (JPA).
- [ ] **Auth Controllers:** Thiết lập các Endpoint đăng ký lập tài khoản (`/register`) và lấy Token (`/login`).
- [ ] **Jwt Generator Service:** Lập trình công cụ lõi JJWT tự build lên một cục Token cấp ra cho User sau Sign in.
- [ ] **Exception Handler:** Đánh chặn lỗi SQL User duplicate (trùng tài khoản) hoặc Sai Password để trả HTTP 400.

---

## 🚀 How to Run

### Yêu cầu hệ thống
* Java 17+
* Chạy máy chủ PostgreSQL cục bộ (port 5432, có sẵn DB `nexus_db`).
* (Nên có) Eureka Server (Đã bật 8761) và API Gateway (8080) để thấy sự phối kết hợp.

### Lệnh khởi chạy nhanh
```bash
# Di chuyển vào thư mục dịch vụ Auth theo Path Root Node của Multi-Module
cd services/auth-service

# Đóng gói nhanh và bỏ qua Test
./mvnw clean package -DskipTests

# Run ứng dụng (Lắng nghe mặc định ở cổng tĩnh 8081)
./mvnw spring-boot:run
```
