package com.nexus.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
    @NotBlank(message = "Product ID is missing or empty") String productId,
    @Positive(message = "Quantity must be greater than zero") int quantity
) {}
