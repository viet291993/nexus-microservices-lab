package com.nexus.orderservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nexus.orderservice.events.model.OrderEventPayload;
import com.nexus.orderservice.events.producer.DefaultServiceEventsProducer;

class OrderProducerServiceTest {

        private DefaultServiceEventsProducer serviceEventsProducer;
        private OrderProducerService orderProducerService;

        @BeforeEach
        void setUp() {
                serviceEventsProducer = mock(DefaultServiceEventsProducer.class);
                orderProducerService = new OrderProducerService(serviceEventsProducer);
        }

        @Test
        void sendOrderEvent_shouldCallProducerWithKafkaMessageKeyHeader() {
                // Arrange
                var payload = new OrderEventPayload();
                payload.setOrderId("ORDER-123");

                when(serviceEventsProducer.sendOrderEvents(
                                org.mockito.ArgumentMatchers.eq(payload),
                                org.mockito.ArgumentMatchers.any())).thenReturn(true);

                // Act
                orderProducerService.sendOrderEvent(payload);

                // Assert
                ArgumentCaptor<DefaultServiceEventsProducer.OrderEventPayloadHeaders> headersCaptor = ArgumentCaptor
                                .forClass(DefaultServiceEventsProducer.OrderEventPayloadHeaders.class);

                verify(serviceEventsProducer)
                                .sendOrderEvents(org.mockito.ArgumentMatchers.eq(payload), headersCaptor.capture());

                var headers = headersCaptor.getValue();
                assertThat(headers).isNotNull();
                assertThat(headers)
                                .containsKey("kafka_messageKey");
                Object rawKey = headers.get("kafka_messageKey");
                assertThat(rawKey).isInstanceOf(byte[].class);
                byte[] keyBytes = (byte[]) rawKey;
                assertThat(new String(keyBytes))
                                .isEqualTo("ORDER-123");
        }

        @Test
        void sendOrderEvent_shouldFailWhenProducerReturnsFalse() {
                var payload = new OrderEventPayload();
                payload.setOrderId("ORDER-123");

                when(serviceEventsProducer.sendOrderEvents(
                                org.mockito.ArgumentMatchers.eq(payload),
                                org.mockito.ArgumentMatchers.any())).thenReturn(false);

                assertThatThrownBy(() -> orderProducerService.sendOrderEvent(payload))
                                .isInstanceOf(IllegalStateException.class);
        }
}
