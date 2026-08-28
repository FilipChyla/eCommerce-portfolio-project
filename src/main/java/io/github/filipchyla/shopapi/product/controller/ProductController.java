package io.github.filipchyla.shopapi.product.controller;

import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Products", description = "Operations related to product catalog")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "name", "createdAt");

    @Operation(
            summary = "Get page of products that match given filters.",
            description = "Fetches all products that fit given filters. Can sort them by name, price or creation time. Not cached"
    )
    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        validateSortFields(pageable);
        Page<ProductResponse> products = productService.findProducts(categoryId, minPrice, maxPrice, pageable);

        return PageResponse.from(products);
    }

    @Operation(
            summary = "Get product by ID",
            description = "Fetches a single product. Result is served from Redis cache if present, otherwise loaded from DB and cached."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported field: " + order.getProperty());
            }
        });
    }
}
