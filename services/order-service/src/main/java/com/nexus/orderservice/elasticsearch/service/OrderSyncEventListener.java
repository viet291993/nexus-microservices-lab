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
        log.info("🔄 [CQRS SYNC] Đồng bộ Order {} sang Elasticsearch với trạng thái: {}",
                event.getOrderId(), event.getStatus());

        OrderDocument document = new OrderDocument(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                event.getStatus());

        searchRepository.save(document);
        log.info("✅ [CQRS SYNC SUCCESS] Đã lưu Order {} vào Elasticsearch Index", event.getOrderId());
    }
}
