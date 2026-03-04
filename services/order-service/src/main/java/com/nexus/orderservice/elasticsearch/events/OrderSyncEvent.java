package com.nexus.orderservice.elasticsearch.events;

import org.springframework.context.ApplicationEvent;

public class OrderSyncEvent extends ApplicationEvent {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final String status;

    public OrderSyncEvent(Object source, String orderId, String productId, int quantity, String status) {
        super(source);
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}
