# Kiến trúc Phân tầng Dịch vụ Định Danh (Identity)
 
Tài liệu giải thích cách thức hoạt động nội bộ cũng như sự tương hỗ giữa bảo mật mã hóa trên **Auth Service** khi tích hợp vào môi trường có **API Gateway**.

---

## 🏛️ 1. Mạch Máu (Flow) Của JWT
Bạn đã bao giờ thắc mắc là "Gateway có lớp `AuthenticationFilter`, nhưng cái Token đó đào đâu ra?"
* Ở **Gateway**, nó đóng vai trò là Lính Xét Vé. Còn ở **Auth Service**, nó chính là Bộ Phận Làm Vé Bán Cửa.
* Nhờ lệnh cấu hình *Route `auth-service`* (ở bài trước) chặn lớp kiểm tra filter, lúc này Gateway cho phép mọi cú đánh API (gọi HTTP Post `/api/v1/auth/login`) trôi tọt vào cổng Server port `8081` này.
* Database ở đây sẽ tự đối soát mã Hash B-Crypt người dùng gõ vào so sánh. Mọi chuyện đúng nghĩa => Chạy qua thư viện *JJWT* bọc thông tin của người dùng (như Chức Danh Admin/User) thành "Vé/Token". Bơm lại Ticket đó cho ứng dụng Front-end.

## 🔐 2. Tại sao gọi là "Stateless"?
* Nếu theo như MVC cũ sử dụng Spring Data/Sessions (Tomcat) => Máy chủ Server tạo một Map trên RAM (Memory Ram) giữ ID người dùng, rồi ghi một mảnh Cookies trên điện thoại của họ. Khi người dùng bấm sang Trang B, web gọi cục cookie đó đính vào request bảo: *À Cookie tên XYZ, đối chiếu Ram tìm mã 002 => Khớp User đó!* Điều này tệ cực độ: Khi có 2 con Máy Chủ song song... Cục Cookie gắn với Máy chủ A sẽ không chạy trên Máy chủ B. Và máy chủ sẽ tốn **rất nhiều RAM** nếu có 1.000.000 phiên đăng nhập!
* Với **Auth Service Architecture (Stateless):** Bạn gọi Token, Server ký Token ném trả bạn và ngay lập tức LÃNG QUÊN bạn là ai. Nó sút trách nhiệm lưu giữ Token đó về chiếc App điện thoại bạn dùng. Lần sau bạn nhấn "Mua Hàng", bạn tự kẹp Token đó lên Request đưa vào API Gateway - Hàm Decoder của Tường lửa sẽ biết: *"Chữ ký này đúng là Server ở nhà mình đúc ra (nhưng méo biết thằng kia là ai, tên gỉ gì gi), nhưng đúng Dấu Đỏ => Chắc chắn là con cháu Auth Service. Cho Pass!"*.
=> Không Máy chủ nào phải cất Session => RAM chạy 100 User hay 1 Tỷ User vẫn nhẹ ngang nhau: Gọi là Stateless Microservices JWT!

---

## 🛠️ 3. Quy tắc mã Passwords (BCrypt)
Một hệ thống Microservices chuẩn không bao giờ được phép nhìn thấy mật khẩu mộc (Plain Text Passwords): `A123456`, `HanhPhuc123`.

Cụm thư viện `spring-security-crypto` được nạp vào File `pom.xml` làm nhiệm vụ lấy mật khẩu bạn gõ vào, trộn với một đoạn Muối Mã Hóa (Random Salt), khuấy nó vô thuật toán để đẻ ra 1 chuỗi dài lòe loẹt như `$2a$10$Gst...vklm13` để cất vào PostgreSQL.
Auth Service, dù bị trộm CSDL, Hacker cũng KHÔNG THỂ giải ngược `Decode` mã kia về thẻ chữ `HanhPhuc123` được (Trừ phi dùng siêu máy tính hì hục dò Hash vài trăm năm). Khi ta thực hiện chức năng `Login` ở lớp Service, ta nộp chữ "HanhPhuc123", JVM sẽ lại trộn và băm một lần nữa, hai bản băm ướm vừa khít nhau => **Trùng khớp mật khẩu mà chả thèm cất chúng nó dạng Text Mộc.**
