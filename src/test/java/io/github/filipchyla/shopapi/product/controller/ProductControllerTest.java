package io.github.filipchyla.shopapi.product.controller;

import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
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
}