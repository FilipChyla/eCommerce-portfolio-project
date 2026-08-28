package io.github.filipchyla.shopapi.product.category.controller;

import io.github.filipchyla.shopapi.product.category.CategoryService;
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

import java.util.UUID;

@Tag(name = "Categories - Admin", description = "Operations altering categories")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Add new category"
    )
    @PostMapping
    public ResponseEntity<SingleCategoryResponse> addCategory(@RequestBody @Valid CreateCategoryRequest categoryData) {
        SingleCategoryResponse response = categoryService.addCategory(categoryData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update category"
    )
    @PatchMapping("/{id}")
    public ResponseEntity<SingleCategoryResponse> updateCategory(@PathVariable UUID id, @RequestBody @Valid UpdateCategoryRequest categoryData) {
        SingleCategoryResponse response = categoryService.updateCategory(id, categoryData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete category. This is hard delete"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}
