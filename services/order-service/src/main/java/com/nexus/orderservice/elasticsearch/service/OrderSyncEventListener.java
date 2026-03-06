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

    private final OrderSearchRepository searchRepository;

    public OrderSyncEventListener(OrderSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Lắng nghe các sự kiện đồng bộ từ Command-side (Luồng ghi)
     * và cập nhật ngay lập tức vào Elasticsearch (Read-side).
     * Đây chính là cơ chế Eventual Consistency cho CQRS.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSyncEvent(OrderSyncEvent event) {
        try {
            String orderId = event.getOrderId();
            OrderStatus newStatus = event.getStatus();

            log.info("🔄 [CQRS SYNC] Nhận event đồng bộ Order {} (Status: {})", orderId, newStatus);

            // Kiểm tra Idempotency: Không ghi đè trạng thái cuối (CONFIRMED/CANCELLED) bằng PENDING
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
        } catch (Exception ex) {
            log.error("❌ [CQRS SYNC ERROR] Lỗi khi xử lý OrderSyncEvent: {}", event, ex);
        }
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
