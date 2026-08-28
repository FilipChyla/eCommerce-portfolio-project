package io.github.filipchyla.shopapi.product.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.product.category.CategoryNotFoundException;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.SingleCategoryResponse;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private CategoryService categoryService;

    public static final String BASE_PATH = "/api/v1/admin/categories";

    @Nested
    class AddCategory {
        @Test
        void addCategory_ShouldCreateCategoryAndReturnsMappedResponse_WhenRequestIsCorrect() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics", null);
            SingleCategoryResponse response = new SingleCategoryResponse(UUID.randomUUID(), "Electronics", Instant.now(), null);

            when(categoryService.addCategory(request)).thenReturn(response);

            mockMvc.perform(post(BASE_PATH)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.parent").doesNotExist());

            verify(categoryService).addCategory(request);
        }

        @Test
        void addCategory_ShouldReturnNotFound_WhenParentIdDoesNotExist() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics", UUID.randomUUID());

            when(categoryService.addCategory(request)).thenThrow(CategoryNotFoundException.class);

            mockMvc.perform(post(BASE_PATH)
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
            SingleCategoryResponse response = new SingleCategoryResponse(UUID.randomUUID(), "Electronics", Instant.now(), null);

            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class))).thenReturn(response);

            mockMvc.perform(patch(BASE_PATH + "/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.parent").doesNotExist());
        }

        @Test
        void update_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", null);

            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class))).thenThrow(CategoryNotFoundException.class);

            mockMvc.perform(patch(BASE_PATH + "/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteCategory {
        @Test
        void delete_ShouldDeleteCategoryAndReturnsConfirmationMessage() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete(BASE_PATH + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Category deleted successfully"));

            verify(categoryService).deleteCategory(id);
        }
    }
}
