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

    public OrderSyncEventListener(OrderSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Lắng nghe các sự kiện đồng bộ từ Command-side (Luồng ghi)
     * và cập nhật ngay lập tức vào Elasticsearch (Read-side).
     * Retry tối đa 3 lần; sau đó log và bỏ qua (DLQ-style: ghi log để xử lý thủ công nếu cần).
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

    private boolean isFinalStatus(String status) {
        return OrderStatus.CONFIRMED.name().equalsIgnoreCase(status) ||
                OrderStatus.CANCELLED.name().equalsIgnoreCase(status);
    }
}
