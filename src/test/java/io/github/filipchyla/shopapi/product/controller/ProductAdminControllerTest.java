package io.github.filipchyla.shopapi.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.category.CategoryNotFoundException;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.dto.UpdateStockRequest;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import io.github.filipchyla.shopapi.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private ProductService productService;

    private static final String BASE_URL = "/api/v1/admin/products";

    @Nested
    class AddProduct {
        @Test
        void addProduct_ShouldCreateProductAndReturnMappedResponse_WhenRequestIsValid() throws Exception {
            // Given
            CreateProductRequest request = new CreateProductRequest(
                    "Laptop", "A laptop", BigDecimal.valueOf(1000), 10, UUID.randomUUID());
            ProductResponse response = new ProductResponse(UUID.randomUUID(), "Laptop", "A laptop",
                    BigDecimal.valueOf(1000), 10, "Electronics", Instant.now());

            when(productService.addProduct(any(CreateProductRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Laptop"));

            verify(productService).addProduct(request);
        }

        @Test
        void addProduct_ShouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
            // Given
            CreateProductRequest request = new CreateProductRequest(
                    "Laptop", "A laptop", BigDecimal.valueOf(1000), 10, UUID.randomUUID());

            when(productService.addProduct(request)).thenThrow(CategoryNotFoundException.class);

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateStock {
        @Test
        void updateStock_ShouldUpdateStockAndReturnMappedResponse_WhenRequestIsValid() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            UpdateStockRequest request = new UpdateStockRequest(42);
            ProductResponse response = new ProductResponse(id, "Laptop", "A laptop",
                    BigDecimal.valueOf(1000), 42, "Electronics", Instant.now());

            when(productService.adjustStock(id, request.difference())).thenReturn(response);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/{id}/stock", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity").value(42));
        }

        @Test
        void updateStock_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            UpdateStockRequest request = new UpdateStockRequest(42);

            when(productService.adjustStock(id, request.difference())).thenThrow(ProductNotFoundException.class);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/{id}/stock", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateProduct {
        @Test
        void updateProduct_ShouldUpdateProductAndReturnMappedResponse_WhenRequestIsValid() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            UpdateProductRequest request = new UpdateProductRequest(
                    "Gaming Laptop", null, null, null, null);
            ProductResponse response = new ProductResponse(id, "Gaming Laptop", "A laptop",
                    BigDecimal.valueOf(1000), 10, "Electronics", Instant.now());

            when(productService.updateProduct(eq(id), any(UpdateProductRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Gaming Laptop"));
        }

        @Test
        void updateProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            UpdateProductRequest request = new UpdateProductRequest(
                    "Gaming Laptop", null, null, null, null);

            when(productService.updateProduct(eq(id), any(UpdateProductRequest.class)))
                    .thenThrow(ProductNotFoundException.class);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateProduct_ShouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            UpdateProductRequest request = new UpdateProductRequest(
                    null, null, null, null, UUID.randomUUID());

            when(productService.updateProduct(eq(id), any(UpdateProductRequest.class)))
                    .thenThrow(CategoryNotFoundException.class);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/{id}", id)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteProduct {
        @Test
        void deleteProduct_ShouldDeleteProductAndReturnConfirmationMessage() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When & Then
            mockMvc.perform(delete(BASE_URL + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Product deleted successfully"));

            verify(productService).deleteProduct(id);
        }
    }
}
