# 🧠 Implementation Notes & Key Learnings

Tài liệu này giải thích các quyết định kiến trúc và công nghệ cốt lõi đằng sau dự án API Gateway.

---

## 1. Tại sao dùng WebFlux thay vì WebMVC?

Gateway chịu trách nhiệm chặn và chuyển tiếp hàng ngàn requests mỗi giây. Sử dụng **Spring WebFlux** với mô hình Non-blocking I/O (qua Netty Server) giúp hệ thống không phải giữ Thread chờ đợi trong quá trình gọi sang service khác.
Nhờ Non-blocking I/O, WebFlux giúp tối đa hóa thông lượng (throughput) mà tiêu tốn cực ít RAM so với kiến trúc 1 Thread / 1 Request của Tomcat (Spring MVC) truyền thống.

## 2. Rate Limiting bằng Redis

Sử dụng thuật toán Token Bucket được cấu hình qua `RedisRateLimiter` gốc của Spring Cloud. Khi có traffic spike (cuộc tấn công hoặc lưu lượng đột biến), Gateway có thể tính toán số lượng Request/giây trên bộ nhớ đệm tốc độ cao của Redis. 
Nếu vượt ngưỡng cài đặt (ví dụ: 10 req/s, burst 20), nó sẽ phân loại là "hết vé" và ném về HTTP Status `429 Too Many Requests` ngay tức thì. Điều này bảo vệ Database và API Services phía sau không bị quá tải.

## 3. Ngắt mạch (Circuit Breaker) với Resilience4j

Tính năng ngắt mạch giúp bảo vệ hệ thống khỏi hiện tượng **"Hiệu ứng Domino"** (lỗi dây chuyền). 
Khi các Service phía sau (Backend) gặp sự cố (bị sập, hoặc kết nối chậm do quá tải DB/Network), Resilience4j sẽ theo dõi và đếm tỷ lệ lỗi/timeout trong một cửa sổ trượt (Sliding Window - ví dụ: 10 requests cấu hình trong `application.yml`).

*   **Đóng mạch (CLOSED):** Hoạt động bình thường.
*   **Mở mạch (OPEN):** Nếu tỷ lệ lỗi vượt ngưỡng 50%, cầu dao sẽ tự động "ngắt". Gateway không cố gọi sang backend bị lỗi nữa, mà điều hướng request ngay lập tức tới bộ phận `FallbackController`. API sẽ trả về trạng thái `503 Service Unavailable` kết hợp với thông báo JSON thân thiện thay vì để request treo (Thread block) chờ đợi trong vô vọng hoặc ném HTTP 500 lỗi từ Server.
*   **Nửa mở (HALF-OPEN):** Sau một thời gian cấu hình (VD: 10s), Resilience4j thử cho lọt vài requests để xem backend đã được hồi phục chưa, nếu OK thì đóng mạch tiếp tục bình thường.
