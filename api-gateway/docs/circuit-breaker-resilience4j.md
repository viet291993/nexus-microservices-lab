# Hệ Thống Ngắt Mạch (Circuit Breaker) với Resilience4j

Tài liệu này giải thích chi tiết về việc ứng dụng mẫu hình kiến trúc **Circuit Breaker** (Ngắt mạch) thông qua thư viện **Resilience4j** bên trong **Nexus API Gateway**.

---

## 🏗️ 1. Tại sao cần Circuit Breaker tại Gateway?

Trong kiến trúc Microservices, API Gateway phụ trách điều hướng hàng ngàn lượt truy cập tới các Backend Services (như User Service, Product Service...). Nhưng điều gì xảy ra nếu một Backend Service bất ngờ gặp sự cố?
- Database quá tải dẫn đến Service phản hồi rất chậm (Timeout).
- Service bị sập hoàn toàn (Crash/Down).

Nếu Gateway cứ tiếp tục chuyển tiếp request đến một Service đang hấp hối:
1. **Thread Blocking:** Gateway sẽ bị treo hàng loạt luồng xử lý (Threads) chỉ để chờ đợi một Service không chịu phản hồi, làm sụp đổ toàn bộ Gateway.
2. **Hiệu ứng Domino:** Sự cố từ một Service sẽ lan rộng và kéo sập các thành phần khác trong hệ thống.
3. **Trải nghiệm người dùng tồi tệ:** Người dùng phải chờ đợi màn hình loading rất lâu để rồi nhận lại một dòng chữ lỗi hệ thống thô kệch `500 Internal Server Error`.

**Giải pháp với Resilience4j:** 
Gateway đóng vai trò như một **Cầu dao điện (Circuit Breaker)**. Nếu phát hiện một Service phía sau bị lỗi quá nhiều hoặc quá chậm, nó sẽ tự động "cúp điện" (ngắt luồng kết nối) tới Service đó. Các request mới sẽ bị chặn lại và trả về một kết quả dự phòng ngay lập tức. Sau một thời gian, nó sẽ chạy thử, nếu Service sống lại thì đóng cầu dao, mọi thứ hoạt động bình thường.

---

## 🛠️ 2. Các trạng thái của Cầu dao (Circuit Breaker)

Resilience4j hoạt động dựa trên 3 trạng thái chính của một State Machine:

### 🟢 CLOSED (Đóng mạch - Bình thường)
Mọi request đều được cho phép chuyển tiếp (route) thông suốt từ Gateway đến các Backend Services. Đồng thời Resilience4j liên tục ghi nhận tỷ lệ lỗi (Error Rate) và tỷ lệ thời gian vượt ngưỡng (Slow Call Rate).

### 🔴 OPEN (Ngắt mạch - Từ chối phục vụ)
Khi tỷ lệ lỗi vượt quá mức cho phép (Ví dụ: > 50% số request ném lỗi hoặc timeout), cầu dao lập tức nhảy sang **OPEN**.
Lúc này, MỌI request hướng tới Backend Service đó sẽ bị chặn ngay tại Gateway để bảo vệ hệ thống. Gateway sẽ phản hồi ngay lập tức cho Client (Front-end/Mobile) thông qua các **Fallback Method** mà không hề gọi tới hệ thống Backend.

### 🟡 HALF_OPEN (Nửa mở - Thử nghiệm)
Sau khi ở trạng thái ngắt mạch (OPEN) trong một khoảng thời gian nhất định (Wait Duration - ví dụ: 10 giây). Cầu dao tự động chuyển sang **HALF_OPEN**.
Ở trạng thái này, Gateway cho lọt lưới một số ít lượng request (ví dụ 5 requests) đi qua làm "chuột bạch" để thăm dò xem Backend Service đã hồi phục hay chưa. 
- Nếu các request này trơn tru ➡️ Trở về trạng thái **CLOSED**.
- Nếu các request này vẫn tịt ngòi ➡️ Lập tức đóng lại thành **OPEN** và tiếp tục chờ đợi.

---

## 🚦 3. Thiết lập Fallback (Graceful Degradation)

Khi cầu dao bị nảy (OPEN), thay vì để người dùng nhận lỗi `500 Internal Error` hoặc các Exception HTTP không rõ nguyên nhân. Hệ thống Gateway đã được lập trình để Route các request bị chặn này sang một nhánh rẽ có sẵn là `FallbackController` (nằm chung tại thư mục `controller`). 

Trong Controller này, hệ thống sẽ trả luôn về một đoạn mã JSON đẹp mắt với mã **503 Service Unavailable** và thông điệp tiếng Việt thân thiện.
* Giúp thiết bị Client biết hệ thống đang bảo trì để hiển thị biểu tượng tải lại (Retry).
* Hoặc hiển thị dữ liệu khuyết một phần thay vì văng hoàn toàn App (Crash).

---

## ⚙️ 4. Chi tiết cấu hình trong `application.yml`

Bản đồ cấu hình đang được thiết lập ở cuối file `application.yml` bên dưới nhánh `resilience4j.circuitbreaker.instances.*`:

- `slidingWindowSize: 10`: Đếm kết quả dựa trên 10 request cuối cùng để đưa ra đánh giá.
- `failureRateThreshold: 50`: Ngưỡng ngắt mạch. Chỉ cần 5/10 request bị hỏng hoặc Time-out thì Cần cẩu trục sẽ nhảy sang mức OPEN.
- `permittedNumberOfCallsInHalfOpenState: 5`: Số lượng máy dò xét (request thử nghiệm) khi ở mức HALF_OPEN.
- `waitDurationInOpenState: 10s`: Sau khi ngắt thì cần đợi 10 giây mới thử gọi sang Backend lần nữa.

Kết hợp cùng mô-đun **TimeLimiter**:
- `timeoutDuration: 3s`: Chờ phản hồi tối đa 3 giây. Từ lúc gửi cho đến lúc lấy dữ liệu từ Backend, nếu vượt qua 3 giây thì Gateway sẽ tính nháp là Timeout (1 Lỗi).
