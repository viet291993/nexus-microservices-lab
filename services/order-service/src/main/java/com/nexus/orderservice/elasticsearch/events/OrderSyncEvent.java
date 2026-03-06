package com.nexus.orderservice.elasticsearch.events;

import com.nexus.orderservice.entity.OrderStatus;
import org.springframework.context.ApplicationEvent;
import java.util.Objects;

public class OrderSyncEvent extends ApplicationEvent {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final OrderStatus status;

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

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
