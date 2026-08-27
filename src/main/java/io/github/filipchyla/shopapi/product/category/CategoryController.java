package io.github.filipchyla.shopapi.product.category;

import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.SingleCategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@Tag(name = "Categories", description = "Operations related to categories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Get categories in a tree-shaped structure",
            description = "Get categories organized hierarchically in a tree structure. The result is cached"
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @Operation(
            summary = "Add new category"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<SingleCategoryResponse> addCategory(@RequestBody @Valid CreateCategoryRequest categoryData) {
        SingleCategoryResponse response = categoryService.addCategory(categoryData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update category"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}")
    public ResponseEntity<SingleCategoryResponse> updateCategory(@PathVariable UUID id, @RequestBody @Valid UpdateCategoryRequest categoryData) {
        SingleCategoryResponse response = categoryService.updateCategory(id, categoryData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete category. This is hard delete"
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}
