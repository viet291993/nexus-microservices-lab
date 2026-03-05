package com.nexus.orderservice.consumer;

import com.nexus.orderservice.entity.OrderEntity;
import com.nexus.orderservice.entity.OrderStatus;
import com.nexus.orderservice.events.model.InventoryResponsePayload;
import com.nexus.orderservice.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryResponseConsumerTest {

    private OrderRepository orderRepository;
    private ApplicationEventPublisher eventPublisher;
    private InventoryResponseConsumer consumer;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        consumer = new InventoryResponseConsumer(orderRepository, eventPublisher);
    }

    @Test
    void processInventoryResponse_shouldConfirmOrderWhenInventoryConfirmed() {
        // Arrange
        var order = new OrderEntity("O001", "P001", 5, OrderStatus.PENDING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(order));

        var payload = new InventoryResponsePayload();
        payload.setOrderId("O001");
        payload.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_CONFIRMED);
        payload.setMessage("OK");

        // Act
        consumer.processInventoryResponse(payload, null);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(eq(order));
    }

    @Test
    void processInventoryResponse_shouldCancelOrderWhenInventoryFailed() {
        // Arrange
        var order = new OrderEntity("O001", "P001", 5, OrderStatus.PENDING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(order));

        var payload = new InventoryResponsePayload();
        payload.setOrderId("O001");
        payload.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED);
        payload.setMessage("Hết hàng");

        // Act
        consumer.processInventoryResponse(payload, null);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(eq(order));
    }

    @Test
    void processInventoryResponse_shouldDoNothingWhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        var payload = new InventoryResponsePayload();
        payload.setOrderId("UNKNOWN");
        payload.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED);
        payload.setMessage("any");

        // Act
        consumer.processInventoryResponse(payload, null);

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processInventoryResponse_shouldBeIdempotentForDuplicateFailureEvent() {
        // Arrange
        var order = new OrderEntity("O001", "P001", 5, OrderStatus.CANCELLED);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(order));

        var payload = new InventoryResponsePayload();
        payload.setOrderId("O001");
        payload.setEventType(InventoryResponsePayload.InventoryEventType.INVENTORY_FAILED);
        payload.setMessage("duplicate failure");

        // Act
        consumer.processInventoryResponse(payload, null);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, never()).save(any());
    }
}
