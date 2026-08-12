package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.PageResponse;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "name", "createdAt");

    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        validateSortFields(pageable);
        Page<ProductResponse> products = productService.findProducts(categoryId, minPrice, maxPrice, pageable).map(productMapper::toProductResponse);
        return PageResponse.from(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productMapper.toProductResponse(productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@RequestBody @Valid CreateProductRequest newProduct) {
        return ResponseEntity.ok(productMapper.toProductResponse(productService.addProduct(newProduct)));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable UUID id, @RequestBody @Valid UpdateStockRequest request) {
        return ResponseEntity.ok(productMapper.toProductResponse(productService.updateStock(id, request.quantity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported field: " + order.getProperty());
            }
        });
    }
}
