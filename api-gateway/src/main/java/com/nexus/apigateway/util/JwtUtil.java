package com.nexus.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    // Default secret key (32 bytes base64 encoded string) cho môi trường Dev.
    // Thực tế nên cấu hình key này trong Config Server hoặc Environment Variables.
    @Value("${jwt.secret:5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437}")
    private String jwtSecret;

    /**
     * Parse và validate chữ ký của token.
     * Nếu token hết hạn, sai định dạng hoặc chữ ký không khớp, nó sẽ quăng các exception (vd: ExpiredJwtException, SignatureException)
     */
    public void validateToken(final String token) {
        Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
