package io.github.filipchyla.shopapi.product.category.controller;

import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.github.filipchyla.shopapi.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private CategoryService categoryService;

    @Nested
    class GetCategories {
        @Test
        void getCategories_ShouldReturnCategoryTreeFromService_WhenCategoriesExist() throws Exception {
            // Given
            CategoryTreeResponse response = new CategoryTreeResponse(UUID.randomUUID(), "Electronics", Instant.now(), List.of());
            when(categoryService.getCategoryTree()).thenReturn(List.of(response));

            // When & Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Electronics"));
        }

        @Test
        void getCategories_ShouldReturnEmptyArray_WhenNoCategoriesExist() throws Exception {
            // Given
            when(categoryService.getCategoryTree()).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void getCategories_ShouldMapNestedTreeToJson_WhenCategoriesHaveDepth() throws Exception {
            // Given
            UUID rootId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            UUID grandchildId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

            CategoryTreeResponse grandchild = new CategoryTreeResponse(grandchildId, "Gaming Laptops", createdAt, new ArrayList<>());
            CategoryTreeResponse child = new CategoryTreeResponse(childId, "Laptops", createdAt, new ArrayList<>(List.of(grandchild)));
            CategoryTreeResponse root = new CategoryTreeResponse(rootId, "Electronics", createdAt, new ArrayList<>(List.of(child)));

            when(categoryService.getCategoryTree()).thenReturn(List.of(root));

            // When & Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(rootId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Electronics"))
                    .andExpect(jsonPath("$[0].createdAt").value("2026-01-01T10:00:00Z"))
                    .andExpect(jsonPath("$[0].children", hasSize(1)))
                    .andExpect(jsonPath("$[0].children[0].id").value(childId.toString()))
                    .andExpect(jsonPath("$[0].children[0].name").value("Laptops"))
                    .andExpect(jsonPath("$[0].children[0].children", hasSize(1)))
                    .andExpect(jsonPath("$[0].children[0].children[0].id").value(grandchildId.toString()))
                    .andExpect(jsonPath("$[0].children[0].children[0].name").value("Gaming Laptops"))
                    .andExpect(jsonPath("$[0].children[0].children[0].children", hasSize(0)));
        }
    }
}