package com.nexus.productservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ProductApiExceptionHandler {

    /**
     * Handle a ResponseStatusException by converting it into a standardized JSON response.
     *
     * @param ex the ResponseStatusException to convert into an HTTP response
     * @return a ResponseEntity whose body is a map containing "status" (HTTP status code) and "error" (reason)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Builds a 400 Bad Request response summarizing field validation errors.
     *
     * @param ex the exception containing binding/validation failures
     * @return a ResponseEntity whose body is a map with keys "status" (400) and
     *         "error" (a semicolon-separated string of "field: message" entries)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
