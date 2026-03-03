package com.nexus.orderservice.entity;

import jakarta.persistence.*;

/**
 * Entity JPA đại diện cho một Đơn hàng trong bảng "orders" của PostgreSQL.
 *
 * Vòng đời trạng thái của Order trong Saga:
 *   PENDING   → Đơn vừa tạo, đang chờ Inventory xử lý trừ kho.
 *   CONFIRMED → Inventory đã trừ kho thành công (INVENTORY_CONFIRMED).
 *   CANCELLED → Inventory từ chối (INVENTORY_FAILED), đơn bị hủy (Saga Rollback).
 */
@Entity
@Table(name = "orders") // Tên bảng trong PostgreSQL. Không dùng "order" vì nó là từ khóa SQL.
public class OrderEntity {

    /**
     * Khóa chính. Sử dụng UUID String thay vì auto-increment Long.
     * Lý do: Trong hệ thống phân tán, UUID đảm bảo không trùng lặp giữa các instance.
     */
    @Id
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    /** Mã sản phẩm khách đặt mua. */
    @Column(name = "product_id", nullable = false)
    private String productId;

    /** Số lượng yêu cầu. */
    @Column(nullable = false)
    private int quantity;

    /**
     * Trạng thái hiện tại của đơn hàng.
     * Giá trị: PENDING, CONFIRMED, CANCELLED.
     */
    @Column(nullable = false)
    private String status;

    // === CONSTRUCTORS === //

    public OrderEntity() {
        // JPA yêu cầu constructor rỗng.
    }

    public OrderEntity(String orderId, String productId, int quantity, String status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    // === GETTERS & SETTERS === //

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "OrderEntity{orderId='" + orderId + "', productId='" + productId +
                "', quantity=" + quantity + ", status='" + status + "'}";
    }
}
