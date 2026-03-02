package com.nexus.apigateway.filter;

import com.nexus.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {
        // Có thể khai báo params custom ở đây nếu muốn inject từ application.yml
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            
            // 1. Kiểm tra nếu Endpoint nằm trong danh sách cần Validate JWT.
            if (validator.isSecured.test(exchange.getRequest())) {
                
                // 2. Chặn Request nếu KHÔNG có chứa Header "Authorization"
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    System.out.println("[Gateway] Missing Authorization header");
                    return onError(exchange.getResponse(), "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                // Lấy ra chuỗi auth bên trong Header (Dùng getFirst an toàn hơn get())
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                
                // 3. Nếu là dạng Bearer Token (phổ biến)
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7); // Xóa chữ "Bearer " để lấy lõi Token JWT
                }
                
                try {
                    // 4. Validate định dạng & Chữ ký Token (Tránh fake token do hacker tự làm)
                    jwtUtil.validateToken(authHeader);
                    
                    // TODO: Tại đây, có thể lấy thông tin username trong JWT để append vào Header của request (Truyền sang các Microservices con phía sau)
                    // exchange.getRequest().mutate().header("loggedInUser", extractedUsername).build();

                } catch (Exception e) {
                    System.out.println("[Gateway] Invalid token: " + e.getMessage());
                    return onError(exchange.getResponse(), "Unauthorized action", HttpStatus.UNAUTHORIZED);
                }
            }
            
            // 5. Nếu Token hợp lệ HOẶC Endpoint public => Chuyển tiếp (Next) Request tới Microservices con
            return chain.filter(exchange);
        });
    }

    // Hàm tiện ích ném mã lỗi không cho phép đi tiếp (thay vì văng HTTP 500 Backend)
    private Mono<Void> onError(ServerHttpResponse response, String message, HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        // Có thể return JSON error format chi tiết hơn ở đây (trong tương lai)
        return response.setComplete();
    }
}
