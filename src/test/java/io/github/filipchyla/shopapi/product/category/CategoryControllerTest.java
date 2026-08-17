package io.github.filipchyla.shopapi.product.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.product.category.dto.CategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CategoryMapper categoryMapper;

    @Nested
    class GetCategories {
        @Test
        void getCategories_ShouldReturnCategoryTreeFromService_WhenCategoriesExist() throws Exception {
            // Given
            CategoryResponse response = new CategoryResponse(UUID.randomUUID(), "Electronics", Instant.now(), List.of());
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

            CategoryResponse grandchild = new CategoryResponse(grandchildId, "Gaming Laptops", createdAt, new ArrayList<>());
            CategoryResponse child = new CategoryResponse(childId, "Laptops", createdAt, new ArrayList<>(List.of(grandchild)));
            CategoryResponse root = new CategoryResponse(rootId, "Electronics", createdAt, new ArrayList<>(List.of(child)));

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

    @Nested
    class AddCategory {
        @Test
        void addCategory_ShouldCreateCategoryAndReturnsMappedResponse_WhenRequestIsCorrect() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics", null);
            Category savedCategory = new Category();
            savedCategory.setId(UUID.randomUUID());
            savedCategory.setName("Electronics");
            CategoryResponse response = new CategoryResponse(savedCategory.getId(), "Electronics", Instant.now(), List.of());

            when(categoryService.addCategory(any(CreateCategoryRequest.class))).thenReturn(savedCategory);
            when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(response);

            mockMvc.perform(post("/api/v1/categories")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Electronics"));

            verify(categoryService).addCategory(request);
        }

        @Test
        void addCategory_ShouldReturnNotFound_WhenParentIdDoesNotExist() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics", UUID.randomUUID());

            when(categoryService.addCategory(request)).thenThrow(CategoryNotFoundException.class);

            mockMvc.perform(post("/api/v1/categories")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateCategory {
        @Test
        void update_ShouldUpdateCategoryAndReturnsMappedResponse_WhenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", null);
            Category updatedCategory = new Category();
            updatedCategory.setId(id);
            updatedCategory.setName("Consumer Electronics");
            CategoryResponse response = new CategoryResponse(id, "Consumer Electronics", Instant.now(), List.of());

            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class))).thenReturn(updatedCategory);
            when(categoryMapper.toCategoryResponse(updatedCategory)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/categories/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Consumer Electronics"));
        }

        @Test
        void update_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", null);
            Category updatedCategory = new Category();
            updatedCategory.setId(id);
            updatedCategory.setName("Consumer Electronics");
            CategoryResponse response = new CategoryResponse(id, "Consumer Electronics", Instant.now(), List.of());

            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class))).thenReturn(updatedCategory);
            when(categoryMapper.toCategoryResponse(updatedCategory)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/categories/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Consumer Electronics"));
        }
    }

    @Nested
    class DeleteCategory {
        @Test
        void delete_ShouldDeleteCategoryAndReturnsConfirmationMessage() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/categories/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Category deleted successfully"));

            verify(categoryService).deleteCategory(id);
        }
    }
}