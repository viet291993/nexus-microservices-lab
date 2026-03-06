package com.nexus.orderservice.elasticsearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nexus.orderservice.entity.OrderStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nexus.orderservice.elasticsearch.entity.OrderDocument;
import com.nexus.orderservice.elasticsearch.events.OrderSyncEvent;
import com.nexus.orderservice.elasticsearch.repository.OrderSearchRepository;

@Service
public class OrderSyncEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncEventListener.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    private final OrderSearchRepository searchRepository;

    /**
     * Create an OrderSyncEventListener backed by the provided search repository.
     *
     * @param searchRepository repository used to query and persist OrderDocument instances in Elasticsearch
     */
    public OrderSyncEventListener(OrderSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Handle an order synchronization event by creating or updating the corresponding Elasticsearch read-side document.
     *
     * Attempts the operation up to MAX_ATTEMPTS with a fixed delay between attempts; on persistent failure logs an error and skips further processing (DLQ-style).
     * If an existing document is in a final status (CONFIRMED or CANCELLED), any differing incoming status is treated idempotently and the update is skipped.
     *
     * @param event the OrderSyncEvent containing the order ID, product/quantity data, and the new OrderStatus
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSyncEvent(OrderSyncEvent event) {
        String orderId = event.getOrderId();
        OrderStatus newStatus = event.getStatus();
        log.info("🔄 [CQRS SYNC] Nhận event đồng bộ Order {} (Status: {})", orderId, newStatus);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                searchRepository.findById(orderId).ifPresentOrElse(existingDoc -> {
                    if (isFinalStatus(existingDoc.getStatus())) {
                        if (!existingDoc.getStatus().equalsIgnoreCase(newStatus.name())) {
                            log.warn("♻️ [CQRS IDEMPOTENT] Bỏ qua cập nhật {} cho Order {} vì đã ở trạng thái cuối: {}",
                                    newStatus, orderId, existingDoc.getStatus());
                        }
                        return;
                    }
                    updateDocument(event);
                }, () -> updateDocument(event));
                return;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("❌ [CQRS SYNC] Lần {} thất bại cho Order {}: {}", attempt, orderId, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("❌ [CQRS SYNC] Retry bị gián đoạn cho Order {}", orderId, ie);
                        return;
                    }
                }
            }
        }
        log.error("❌ [CQRS SYNC ERROR] Đã thử {} lần vẫn lỗi, bỏ qua (có thể xử lý lại từ nguồn khác): orderId={}", MAX_ATTEMPTS, orderId, lastException);
    }

    /**
     * Creates or replaces the search-side OrderDocument from the given event and persists it to Elasticsearch.
     *
     * @param event the synchronization event containing orderId, productId, quantity, and status used to build the document
     */
    private void updateDocument(OrderSyncEvent event) {
        OrderDocument document = new OrderDocument(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                event.getStatus().name());

        searchRepository.save(document);
        log.info("✅ [CQRS SYNC SUCCESS] Đã lưu Order {} vào Elasticsearch Index (Status: {})",
                event.getOrderId(), event.getStatus());
    }

    /**
     * Determine whether the given status string represents a final order state.
     *
     * @param status the status name to check (case-insensitive)
     * @return `true` if the status equals `CONFIRMED` or `CANCELLED` (case-insensitive), `false` otherwise
     */
    private boolean isFinalStatus(String status) {
        return OrderStatus.CONFIRMED.name().equalsIgnoreCase(status) ||
                OrderStatus.CANCELLED.name().equalsIgnoreCase(status);
    }
}
