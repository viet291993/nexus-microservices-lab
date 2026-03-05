package com.nexus.orderservice.controller;

import java.util.List;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.orderservice.elasticsearch.entity.OrderDocument;
import com.nexus.orderservice.elasticsearch.repository.OrderSearchRepository;

/**
 * Controller chuyên biệt xử lý các yêu cầu Truy vấn (Query) của CQRS.
 * Mọi request GET sẽ được trỏ tới đây để đọc dữ liệu siêu tốc từ Elasticsearch.
 */
@RestController
@RequestMapping("/api/v1/orders/query")
public class OrderQueryController {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryController.class);
    private final OrderSearchRepository searchRepository;

    public OrderQueryController(OrderSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * API tìm kiếm đơn hàng trong Elasticsearch (Fast Read).
     */
    @GetMapping("/search")
    public ResponseEntity<List<OrderDocument>> searchOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productId) {

        log.info("🔍 [CQRS QUERY] Tìm kiếm đơn hàng với orderId={}, status={}, productId={}", 
                orderId, status, productId);

        if (orderId != null) {
            return searchRepository.findById(orderId)
                    .map(doc -> ResponseEntity.ok(List.of(doc)))
                    .orElse(ResponseEntity.ok(List.of()));
        }

        List<OrderDocument> results;
        if (status != null && productId != null) {
            results = searchRepository.findByStatusAndProductId(status, productId);
        } else if (status != null) {
            results = searchRepository.findByStatus(status);
        } else if (productId != null) {
            results = searchRepository.findByProductId(productId);
        } else {
            results = StreamSupport.stream(searchRepository.findAll().spliterator(), false)
                    .toList();
        }

        return ResponseEntity.ok(results);
    }
}
