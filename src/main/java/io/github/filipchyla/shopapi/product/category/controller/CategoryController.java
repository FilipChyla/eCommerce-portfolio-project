package io.github.filipchyla.shopapi.product.category.controller;

import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
}
