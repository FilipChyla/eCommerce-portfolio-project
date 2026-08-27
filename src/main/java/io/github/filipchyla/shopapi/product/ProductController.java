package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.dto.*;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(
            summary = "Add new product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@RequestBody @Valid CreateProductRequest newProduct) {
        return ResponseEntity.ok(productService.addProduct(newProduct));
    }

    @Operation(
            summary = "Change stock amount",
            description = "Adjusts stock amount of a product by given amount. Can be negative to decrease stock amount. Updates cache"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable UUID id, @RequestBody @Valid UpdateStockRequest request) {
        return ResponseEntity.ok(productService.adjustStock(id, request.difference()));
    }

    @Operation(
            summary = "Update product information",
            description = "Update info for fields in request, not mentioned fields will be ignored. Updates cache"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id, @RequestBody @Valid UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(
            summary = "Delete product. This is a soft delete"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new MessageResponse("Product deleted successfully"));
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported field: " + order.getProperty());
            }
        });
    }
}
