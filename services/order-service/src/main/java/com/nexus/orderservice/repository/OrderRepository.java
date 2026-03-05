package com.nexus.orderservice.repository;

import com.nexus.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy xuất dữ liệu bảng "orders" trong PostgreSQL.
 *
 * Kế thừa JpaRepository => Tự động có sẵn các method CRUD:
 *   - save(entity)       : Lưu/Cập nhật đơn hàng.
 *   - findById(orderId)  : Tìm đơn theo mã UUID.
 *   - findAll()          : Lấy toàn bộ danh sách đơn.
 *   - deleteById(orderId): Xóa đơn hàng.
 *
 * <String> = Kiểu dữ liệu của khóa chính (orderId là String UUID).
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Hiện tại chưa cần custom query. JpaRepository đã cung cấp đủ.
    // Sau này có thể thêm: List<OrderEntity> findByStatus(String status);
}
