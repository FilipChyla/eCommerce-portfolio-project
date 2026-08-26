package io.github.filipchyla.shopapi.product;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private ProductService productService;

    private static final String BASE_URL = "/api/v1/products";

    @Nested
    class GetProducts {
        @Test
        void getProducts_ShouldReturnPageOfProducts_WhenNoFiltersProvided() throws Exception {
            // Given
            ProductResponse product = new ProductResponse(
                    UUID.randomUUID(),
                    "Existing product",
                    "Existing description",
                    BigDecimal.valueOf(50),
                    10,
                    "Category Name",
                    Instant.now()
            );
            Page<ProductResponse> page = new PageImpl<>(List.of(product));

            when(productService.findProducts(isNull(), isNull(), isNull(), any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Existing product"));
        }

        @Test
        void getProducts_ShouldReturnEmptyContent_WhenNoProductsExist() throws Exception {
            // Given
            when(productService.findProducts(isNull(), isNull(), isNull(), any())).thenReturn(Page.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        void getProducts_ShouldPassFiltersToService_WhenCategoryAndPriceRangeProvided() throws Exception {
            // Given
            UUID categoryId = UUID.randomUUID();
            BigDecimal minPrice = BigDecimal.valueOf(10);
            BigDecimal maxPrice = BigDecimal.valueOf(100);

            when(productService.findProducts(eq(categoryId), eq(minPrice), eq(maxPrice), any()))
                    .thenReturn(Page.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL)
                            .param("categoryId", categoryId.toString())
                            .param("minPrice", minPrice.toString())
                            .param("maxPrice", maxPrice.toString()))
                    .andExpect(status().isOk());

            verify(productService).findProducts(eq(categoryId), eq(minPrice), eq(maxPrice), any());
        }

        @Test
        void getProducts_ShouldReturnBadRequest_WhenSortFieldIsNotAllowed() throws Exception {
            // Given & When & Then
            mockMvc.perform(get(BASE_URL).param("sort", "unsupportedField,desc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productService);
        }
    }

    @Nested
    class GetProduct {
        @Test
        void getProduct_ShouldReturnProduct_WhenProductExists() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            ProductResponse product = new ProductResponse(id, "Laptop", "A laptop",
                    BigDecimal.valueOf(1000), 10, "Electronics", Instant.now());

            when(productService.getProductById(id)).thenReturn(product);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Laptop"));
        }

        @Test
        void getProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            when(productService.getProductById(id)).thenThrow(ProductNotFoundException.class);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

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