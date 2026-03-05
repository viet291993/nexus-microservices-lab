package com.nexus.orderservice.elasticsearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
    @EventListener
    public void handleOrderSyncEvent(OrderSyncEvent event) {
        String orderId = event.getOrderId();
        String newStatus = event.getStatus();

        log.info("🔄 [CQRS SYNC] Nhận event đồng bộ Order {} (Status: {})", orderId, newStatus);

        // Kiểm tra Idempotency: Không ghi đè trạng thái cuối (CONFIRMED/CANCELLED) bằng PENDING
        searchRepository.findById(orderId).ifPresentOrElse(existingDoc -> {
            if (isFinalStatus(existingDoc.getStatus()) && "PENDING".equalsIgnoreCase(newStatus)) {
                log.warn("♻️ [CQRS IDEMPOTENT] Bỏ qua cập nhật PENDING cho Order {} vì đã ở trạng thái cuối: {}",
                        orderId, existingDoc.getStatus());
            } else {
                updateDocument(event);
            }
        }, () -> {
            updateDocument(event);
        });
    }

    private void updateDocument(OrderSyncEvent event) {
        OrderDocument document = new OrderDocument(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                event.getStatus());

        searchRepository.save(document);
        log.info("✅ [CQRS SYNC SUCCESS] Đã lưu Order {} vào Elasticsearch Index (Status: {})",
                event.getOrderId(), event.getStatus());
    }

    private boolean isFinalStatus(String status) {
        return "CONFIRMED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
    }
}
