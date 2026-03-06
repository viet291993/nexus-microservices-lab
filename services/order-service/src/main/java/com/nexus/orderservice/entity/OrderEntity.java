package com.nexus.orderservice.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity JPA đại diện cho một Đơn hàng trong bảng "orders" của PostgreSQL.
 *
 * Vòng đời trạng thái của Order trong Saga:
 * PENDING → Đơn vừa tạo, đang chờ Inventory xử lý trừ kho.
 * CONFIRMED → Inventory đã trừ kho thành công (INVENTORY_CONFIRMED).
 * CANCELLED → Inventory từ chối (INVENTORY_FAILED), đơn bị hủy (Saga Rollback).
 */
@Entity
@Table(name = "orders") // Tên bảng trong PostgreSQL. Không dùng "order" vì nó là từ khóa SQL.
public class OrderEntity {

    /**
     * Khóa chính. Sử dụng UUID String thay vì auto-increment Long.
     * Lý do: Trong hệ thống phân tán, UUID đảm bảo không trùng lặp giữa các
     * instance.
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Version
    private Long version;

    // === CONSTRUCTORS === //

    public OrderEntity() {
        // JPA yêu cầu constructor rỗng.
    }

    public OrderEntity(String orderId, String productId, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    /**
     * Gets the order's unique identifier.
     *
     * @return the order identifier as a UUID-style string
     */

    public String getOrderId() {
        return orderId;
    }

    /**
     * Set the entity's primary key.
     *
     * @param orderId the UUID-style primary key for the order; should be non-null and unique
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the product identifier associated with this order.
     *
     * @return the product identifier for this order, or `null` if not set
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Sets the product identifier associated with this order.
     *
     * @param productId the product id for the order; must not be null
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Gets the quantity of items in the order.
     *
     * @return the quantity of items for this order
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the number of items for this order.
     *
     * @param quantity the quantity of the product in the order
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Retrieves the current status of the order.
     *
     * @return the order's status (`OrderStatus`), e.g. `PENDING`, `CONFIRMED`, or `CANCELLED`
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Set the order's status.
     *
     * <p>Expected values:
     * <ul>
     *   <li>PENDING — new order awaiting inventory deduction</li>
     *   <li>CONFIRMED — inventory deduction succeeded</li>
     *   <li>CANCELLED — inventory denied; used during saga rollback</li>
     * </ul>
     *
     * @param status the new OrderStatus for this order
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Get the optimistic locking version for this entity.
     *
     * @return the version value used for optimistic locking, or `null` if not set
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Set the optimistic-locking version for this entity.
     *
     * @param version the version number used for optimistic locking; may be {@code null} before the entity is persisted
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Provide a concise string representation of the order, including orderId, productId, quantity, and status.
     *
     * @return a string formatted as "OrderEntity{orderId='...', productId='...', quantity=..., status='...'}"
     */
    @Override
    public String toString() {
        return "OrderEntity{orderId='" + orderId + "', productId='" + productId +
                "', quantity=" + quantity + ", status='" + status + "'}";
    }

    /**
     * Determine equality between this OrderEntity and another object based on the entity's `orderId`.
     *
     * @param o the object to compare with this OrderEntity
     * @return `true` if `o` is an OrderEntity and both have non-null, equal `orderId` values, `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderEntity that = (OrderEntity) o;
        if (this.orderId == null || that.orderId == null) {
            return false;
        }
        return Objects.equals(orderId, that.orderId);
    }

    /**
     * Computes the hash code for this entity based on its primary identifier.
     *
     * Uses the hash of `orderId` when `orderId` is non-null; otherwise returns this object's identity hash code.
     *
     * @return the hash code derived from `orderId` when available, otherwise the object's identity hash code
     */
    @Override
    public int hashCode() {
        return orderId == null ? System.identityHashCode(this) : Objects.hash(orderId);
    }
}
