package com.nexus.orderservice.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "orders", createIndex = false)
public class OrderDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String orderId;

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Integer)
    private Integer quantity;

    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * Creates an empty OrderDocument instance.
     *
     * Fields are left unset (null) and should be initialized via setters or the parameterized constructor.
     */
    public OrderDocument() {
    }

    /**
     * Constructs an OrderDocument with the provided values and sets the document id to the given orderId.
     *
     * @param orderId   the order identifier; also used as the document id
     * @param productId the product identifier associated with the order
     * @param quantity  the quantity ordered, or `null` if unspecified
     * @param status    the order status
     */
    public OrderDocument(String orderId, String productId, Integer quantity, String status) {
        this.id = orderId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.orderId = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
        this.id = orderId;
    }

    public String getProductId() {
        return productId;
    }

    /**
     * Set the product identifier associated with this order.
     *
     * @param productId the product identifier to associate with the order, may be null
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Gets the quantity of the order.
     *
     * @return the quantity of the order, or `null` if not set
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Set the order's quantity.
     *
     * @param quantity the quantity of product for the order, or `null` if unspecified
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
