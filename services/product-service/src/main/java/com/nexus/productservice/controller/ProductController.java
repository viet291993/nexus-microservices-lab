package com.nexus.productservice.controller;

import com.nexus.productservice.entity.Product;
import com.nexus.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * Retrieve a product by its identifier.
     *
     * @param id the product identifier
     * @return a ResponseEntity containing the found Product and HTTP 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Creates a new product from the provided request body and persists it.
     *
     * @param product the product data from the request body; validated before creation
     * @return a ResponseEntity containing the created Product and HTTP 201 Created status
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates the product identified by the given id using the provided product data.
     *
     * @param id      the id of the product to update
     * @param product the product data to apply; validated against bean constraints
     * @return the updated Product
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
