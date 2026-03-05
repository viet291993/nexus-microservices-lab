package com.nexus.orderservice.elasticsearch.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.nexus.orderservice.elasticsearch.entity.OrderDocument;

@Repository
public interface OrderSearchRepository extends ElasticsearchRepository<OrderDocument, String> {

    List<OrderDocument> findByStatus(String status);

    List<OrderDocument> findByProductId(String productId);

    List<OrderDocument> findByStatusAndProductId(String status, String productId);
}
