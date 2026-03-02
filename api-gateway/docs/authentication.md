# Phân tích luồng Global Authentication Filter (JWT)

Tài liệu giải thích cách cài đặt và hoạt động của kiến trúc bảo mật (Security Layer) bên trong hệ thống **Nexus API Gateway**. Theo mô hình này, tất cả các Request đều phải đi qua chốt chặn duy nhất thay vì thực hiện lặp đi lặp lại xác thực (Authentication) tại mỗi Microservice.

---

## 🏗️ 1. Tại sao lại cấu hình JWT ở Gateway?
Trong cấu trúc Microservice phân tán, nếu mỗi Service (User, Product, Order, Inventory...) đều tự viết thư viện và Validate Token:
* **Khủng hoảng dư thừa (Redundancy):** Lặp lại code thư viện nén Token dải rác khắp hàng chục projects lớn nhỏ.
* **Gây lỗ hổng nghiệp vụ:** Bất kỳ service mới nào quên tích hợp hoặc code nhầm logic, nó có thể bị khai thác (Exploit) trái phép.
* **Tải CPU lãng phí:** Gây quá tải việc tính mật mã băm trên toàn cụm (hashing bottleneck).

**Giải pháp API Gateway (Global/Custom Filter):** 
API Gateway là "cánh cổng đầu vào duy nhất". Mọi API Client gửi (trừ lệnh Login lấy Token ban đầu) sẽ bị chặn lại ở cánh cửa này. Token có hợp lệ thì request mới được thâm nhập sang các phân vùng Microservices Backend; Nếu Token sai hoặc hết hạn, Gateway **trả ngay mã lỗi chuẩn HTTP 401 Unauthorized** tiết kiệm băng thông nội bộ.

---

## 🛠️ 2. Các thành phần cốt lõi của Filter

### A. Công lý giải mã - `JwtUtil.java`
Nằm trong thư mục `util`. Nó là cốt lõi của bảo mật mạng với khóa mã hóa bí mật (Secret Key). 
Bất kỳ JWT Token xịn nào cũng có 3 phần. Khi chuỗi Token từ người dùng đưa tới, hàm `validateToken()` sẽ dùng mã Secret để gỡ niêm phong phần "Chữ ký số". Nếu bị tin tặc dùng Key khác Hash lại hoặc đã hết thời gian sử dụng, Java sẽ lập tức nhảy (Throw) ra `SignatureException` hoặc `ExpiredJwtException`.

### B. Vùng miễn trừ - `RouteValidator.java`
Không phải 100% đường dẫn đều giăng dây an toàn. Để lấy được Token thì User phải gọi được API `/api/v1/auth/login`. Do đó, Class này cung cấp một danh sách "White-list" (các Endpoint công khai không kẹp filter).

### C. Kẻ gác cổng - `AuthenticationFilter.java`
Class trọng tâm kế thừa từ `AbstractGatewayFilterFactory` của Spring. Mỗi khi có lượt truy cập (exchange), quá trình sẽ đi qua:
1. Hỏi `RouteValidator` xem trang này thuộc vùng kín hay vùng miễn trừ.
2. Nếu mở (Public): Cho qua. 
3. Nếu kín (Secured): Vét tệp Headers tìm key có tên `Authorization`.
4. Quét xem có định dạng bắt đầu bằng chữ `Bearer <Chuỗi Token>` thay vì basic Auth.
5. Cắt bỏ râu ria, ném token thô đi hỏi `JwtUtil`.
6. Tốt: Cho phép Gateway điều phối nó sang (ví dụ `User Service`) thông qua lệnh `chain.filter()`.
7. Lỗi/Bất Hợp Lệ: Call ngay logic phụ trả về `401`.

---

## 🚦 3. Lưu ý việc Mapping trong YAML
Phần Logic code đã hoàn thiện nhưng để hệ thống tự động Hook (Kích hoạt) vào đúng Service, tên của Class (`AuthenticationFilter`) bắt buộc phải được khai dưới khóa danh mục `filters:` nằm ở file biến cấu hình YAML (`application.yml`) của Spring Cloud Gateway.

Để xem thêm định dạng Mapping của file `application.yml` xin kiểm tra chính tả các dòng chú thích đã được thêm bên trong File.
