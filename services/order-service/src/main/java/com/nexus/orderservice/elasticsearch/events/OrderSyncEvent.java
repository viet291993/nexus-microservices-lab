package com.nexus.orderservice.elasticsearch.events;

import com.nexus.orderservice.entity.OrderStatus;
import org.springframework.context.ApplicationEvent;
import java.util.Objects;

public class OrderSyncEvent extends ApplicationEvent {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final OrderStatus status;

    /**
     * Create an OrderSyncEvent representing a synchronization event for an order.
     *
     * @param orderId   the non-null identifier of the order
     * @param productId the non-null identifier of the product
     * @param quantity  the quantity for the event; must be greater than or equal to zero
     * @param status    the non-null status of the order
     * @throws NullPointerException     if {@code orderId}, {@code productId}, or {@code status} is null
     * @throws IllegalArgumentException if {@code quantity} is negative
     */
    public OrderSyncEvent(Object source, String orderId, String productId, int quantity, OrderStatus status) {
        super(source);
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        this.quantity = quantity;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    /**
     * Returns the quantity of items associated with this order synchronization event.
     *
     * @return the quantity for the event; zero or greater
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the status of this order synchronization event.
     *
     * @return the event's {@link com.nexus.orderservice.entity.OrderStatus} (never null)
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Create a string representation of the event including orderId, productId, quantity, status, and source.
     *
     * @return the `String` representation of this OrderSyncEvent containing orderId, productId, quantity, status, and source
     */
    @Override
    public String toString() {
        return "OrderSyncEvent{" +
                "orderId='" + orderId + '\'' +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", status=" + status +
                ", source=" + getSource() +
                '}';
    }
}
