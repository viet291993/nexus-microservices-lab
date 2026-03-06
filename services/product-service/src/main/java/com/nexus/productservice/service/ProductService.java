package com.nexus.productservice.service;

import com.nexus.productservice.entity.Product;
import com.nexus.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public Product getProductById(String id) {
        log.info("🔍 [DB] Fetching product from MongoDB: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    @Cacheable(value = "productList", sync = true)
    public List<Product> getAllProducts() {
        log.info("🔍 [DB] Fetching all products from MongoDB");
        return productRepository.findAll();
    }

    @CachePut(value = "products", key = "#result.id", unless = "#result == null || #result.id == null")
    @CacheEvict(value = "productList", allEntries = true)
    public Product createProduct(Product product) {
        log.info("➕ [DB] Creating product in MongoDB: {}", product.getName());
        return productRepository.save(product);
    }

    @CachePut(value = "products", key = "#id", unless = "#result == null")
    @CacheEvict(value = "productList", allEntries = true)
    public Product updateProduct(String id, Product productDetails) {
        log.info("📝 [DB] Updating product in MongoDB: {}", id);
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        return productRepository.save(product);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productList", allEntries = true)
    })
    public void deleteProduct(String id) {
        log.info("🗑️ [DB] Deleting product from MongoDB: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}
