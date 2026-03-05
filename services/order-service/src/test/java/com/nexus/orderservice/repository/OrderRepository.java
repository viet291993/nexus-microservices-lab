package com.nexus.orderservice.repository;

import com.nexus.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Test-side mirror of the main OrderRepository interface.
 *
 * Một số plugin generate source (ZenWave) có thể ảnh hưởng thứ tự biên dịch,
 * khiến test không truy cập được class file của OrderRepository ở main source.
 * Định nghĩa lại interface cùng chữ ký trong test scope giúp compiler có đủ
 * thông tin type mà không thay đổi hành vi runtime (Spring Data JPA vẫn tạo
 * repository như cũ).
 */
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
}

