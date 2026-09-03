package io.github.filipchyla.shopapi.product.controller;

import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.dto.UpdateStockRequest;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Products - Admin", description = "Operations altering product catalog")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;

    @Operation(
            summary = "Add new product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@RequestBody @Valid CreateProductRequest newProduct) {
        ProductResponse createdProduct = productService.addProduct(newProduct);
        URI uri = URI.create("/api/v1/products/" + createdProduct.id());

        return ResponseEntity.created(uri)
                .body(createdProduct);
    }

    @Operation(
            summary = "Change stock amount",
            description = "Adjusts stock amount of a product by given amount. Can be negative to decrease stock amount. Updates cache"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable UUID id, @RequestBody @Valid UpdateStockRequest request) {
        return ResponseEntity.ok(productService.adjustStock(id, request.difference()));
    }

    @Operation(
            summary = "Update product information",
            description = "Update info for fields in request, not mentioned fields will be ignored. Updates cache"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id, @RequestBody @Valid UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(
            summary = "Delete product. This is a soft delete"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new MessageResponse("Product deleted successfully"));
    }
}
