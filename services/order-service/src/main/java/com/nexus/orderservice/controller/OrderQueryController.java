package com.nexus.orderservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Searches orders in Elasticsearch using optional filters and returns matching documents.
     *
     * <p>If `orderId` is provided, the response contains either the matching document in a single-element
     * list or an empty list if not found. Otherwise, results are determined by the presence of
     * `status` and/or `productId`:
     * <ul>
     *   <li>both `status` and `productId` — returns documents matching both filters</li>
     *   <li>only `status` — returns documents matching the status</li>
     *   <li>only `productId` — returns documents matching the productId</li>
     *   <li>no filters — returns a paginated subset of all documents</li>
     * </ul>
     *
     * @param orderId   optional order identifier; when present the search returns at most the matching document
     * @param status    optional order status filter
     * @param productId optional product identifier filter
     * @param page      page index for pagination when no filters are provided; negative values are treated as 0
     * @param size      page size for pagination when no filters are provided; values are clamped to the range 1–100
     * @return          a list of matching OrderDocument instances; may be empty
     */
    @GetMapping("/search")
    public ResponseEntity<List<OrderDocument>> searchOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String safeOrderId = sanitizeForLog(orderId);
        String safeStatus = sanitizeForLog(status);
        String safeProductId = sanitizeForLog(productId);

        log.info("🔍 [CQRS QUERY] Tìm kiếm đơn hàng với orderId={}, status={}, productId={}",
                safeOrderId, safeStatus, safeProductId);

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
            int safeSize = Math.min(Math.max(size, 1), 100);
            int safePage = Math.max(page, 0);
            Pageable pageable = PageRequest.of(safePage, safeSize);
            Page<OrderDocument> pageResult = searchRepository.findAll(pageable);
            results = pageResult.getContent();
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Normalize a string for safe inclusion in logs by replacing control characters and limiting length.
     *
     * Replaces newline, carriage return, tab and other control characters with underscores and truncates the result to at most 200 characters.
     *
     * @param value the input string to sanitize; may be null
     * @return the sanitized string, or {@code null} if {@code value} is {@code null}
     */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", "_")
                .replaceAll("\\p{Cntrl}", "_");
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        return sanitized;
    }
}
