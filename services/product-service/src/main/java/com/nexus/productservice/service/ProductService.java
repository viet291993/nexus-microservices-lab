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

    /**
     * Persist a new product and refresh the affected product caches.
     *
     * <p>The saved product is stored in the repository; the individual product cache is updated
     * and the cached product list is evicted to maintain consistency.</p>
     *
     * @param product the product to persist
     * @return the persisted Product, typically with its `id` populated by the datastore
     */
    @CachePut(value = "products", key = "#result.id", unless = "#result == null || #result.id == null")
    @CacheEvict(value = "productList", allEntries = true)
    public Product createProduct(Product product) {
        log.info("➕ [DB] Creating product in MongoDB: {}", product.getName());
        return productRepository.save(product);
    }

    /**
     * Updates an existing product's fields and persists the changes.
     *
     * @param id             the identifier of the product to update
     * @param productDetails the source of updated values (name, description, price, stock)
     * @return                the persisted Product after update
     * @throws ResponseStatusException if no product with the given id exists (HTTP 404)
     */
    @CachePut(value = "products", key = "#id", unless = "#result == null")
    @CacheEvict(value = "productList", allEntries = true)
    public Product updateProduct(String id, Product productDetails) {
        log.info("📝 [DB] Updating product in MongoDB: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        return productRepository.save(product);
    }

    /**
     * Delete the product with the given ID and evict related product caches.
     *
     * Deletes the product from the repository and removes the corresponding entry
     * from the "products" cache and clears the "productList" cache.
     *
     * @param id the identifier of the product to delete
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 404 NOT_FOUND if no product exists for the given id
     */
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
