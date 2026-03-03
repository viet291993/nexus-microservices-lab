# 🧪 Testing Guide — API Gateway

Tài liệu này mô tả chiến lược kiểm thử, cấu trúc test suite, và cách chạy từng nhóm test của module **API Gateway**.

---

## Yêu cầu trước khi chạy

| Loại Test | Redis | Eureka Server |
|---|:---:|:---:|
| `@SpringBootTest` (Integration) | ✅ Cần | ✅ Cần |
| `@WebFluxTest` (Unit) | ❌ Không cần | ❌ Không cần |

Khởi động nhanh Redis bằng Docker nếu chưa có:
```bash
docker run -d -p 6379:6379 redis:alpine
```

---

## Lệnh chạy

```bash
# Chạy toàn bộ test suite (7 test cases)
mvn test

# Chạy riêng từng class
mvn test -Dtest=ApiGatewayApplicationTests
mvn test -Dtest=CorsConfigTest
mvn test -Dtest=FallbackControllerTest

# Chạy riêng một method
mvn test -Dtest=CorsConfigTest#testCorsConfigurationRules
```

---

## Cấu trúc Test Suite

```
src/test/java/com/nexus/apigateway/
├── ApiGatewayApplicationTests.java   → Integration: JWT Auth end-to-end  (2 tests)
├── CorsConfigTest.java               → Unit: CORS Headers & Config        (1 test)
└── FallbackControllerTest.java       → Unit: Fallback endpoints           (4 tests)
```

---

## Chi tiết từng Test Class

### 1. `ApiGatewayApplicationTests` — Bảo mật JWT End-to-End

**Loại:** `@SpringBootTest` (Integration Test)
**Mục tiêu:** Kiểm tra toàn bộ luồng request qua Security Filter của Gateway.

| Test Method | Kịch bản | HTTP Status kỳ vọng |
|---|---|:---:|
| `testUnauthorizedRequest_ShouldReturn401` | Gửi request **không có** Bearer Token | `401` |
| `testAuthorizedRequest_ShouldReturn503Fallback` | Gửi request **có** JWT mock, backend chưa chạy → Circuit Breaker kích hoạt | `503` |

**Lý do quan trọng:** Đây là test cuối cùng xác nhận rằng không một request nào lọt được vào backend mà không qua lớp xác thực JWT.

---

### 2. `CorsConfigTest` — Cấu hình CORS

**Loại:** `Plain JUnit` (Unit Test)
**Mục tiêu:** Đảm bảo `CorsConfigurationSource` trả về đúng quyền truy cập CORS headers, giúp trình duyệt Frontend không bị chặn khi gọi API cross-origin. Spring WebFlux Security có nhiều quy tắc phức tạp với Integration Test, việc Unit Test logic object giúp đảm bảo mã phủ sóng 100% nhưng loại bỏ tỷ lệ báo lỗi giả (false positive HTTP 403).

| Test Method | Kịch bản | Kỳ vọng |
|---|---|---|
| `testCorsConfigurationRules` | Inject `MockServerWebExchange` từ localhost:3000 vào Bean Source | Đảm bảo Origin, Pattern, MaxAge, AllowedMethod được mapping chính xác. |

---

### 3. `FallbackControllerTest` — Fallback Endpoints (Circuit Breaker)

**Loại:** `@WebFluxTest` (Unit Test — không cần Redis, Eureka)
**Mục tiêu:** Kiểm tra trực tiếp `FallbackController` trả về đúng cấu trúc JSON khi Circuit Breaker ngắt mạch.

| Test Method | Endpoint | HTTP Status | JSON fields kỳ vọng |
|---|---|:---:|---|
| `testUserServiceFallback_ShouldReturn503` | `/fallback/userServiceFallback` | `503` | — |
| `testUserServiceFallback_ShouldReturnCorrectJsonBody` | `/fallback/userServiceFallback` | `503` | `status`, `error`, `message` |
| `testProductServiceFallback_ShouldReturn503` | `/fallback/productServiceFallback` | `503` | — |
| `testProductServiceFallback_ShouldReturnCorrectJsonBody` | `/fallback/productServiceFallback` | `503` | `status`, `error`, `message` |

---

## Phạm vi bao phủ (Coverage Summary)

| Source Class | Được test bởi | Mức phủ hành vi |
|---|---|:---:|
| `SecurityConfig.java` | `ApiGatewayApplicationTests` | ✅ Cao |
| `CorsConfig.java` | `CorsConfigTest` | ✅ Cao |
| `FallbackController.java` | `FallbackControllerTest` | ✅ Đầy đủ |
| `RateLimiterConfig.java` | Loaded qua `@SpringBootTest` context | ⚠️ Một phần |

> 💡 `RateLimiterConfig` được khởi tạo trong các Integration Test nhưng chưa có test case riêng giả lập Redis để kích hoạt logic `429 Too Many Requests`. Đây là điểm có thể bổ sung trong tương lai với **Embedded Redis** hoặc **Testcontainers Redis**.
