package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryNotFoundException;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductServiceCacheTest.CacheTestConfig.class)
class ProductServiceCacheTest {
    @Autowired
    private ProductService productService;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private ProductMapper productMapper;

    private static final String CACHE_NAME = "products";

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        cache.clear();
    }

    private Cache getProductsCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        return cache;
    }

    private Product buildProduct(UUID id) {
        Product product = new Product();
        product.setId(id);
        product.setStockQuantity(2);
        return product;
    }

    private Object getCachedValue(UUID id) {
        Cache.ValueWrapper wrapper = getProductsCache().get(id);
        assertThat(wrapper).isNotNull();
        return wrapper.get();
    }

    @Nested
    class GetProductById {
        @Test
        void getProductById_ShouldCallRepositoryOnlyOnce_WhenCalledMultipleTimesWithSameId() {
            // Given
            UUID id = UUID.randomUUID();
            Product product = buildProduct(id);

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(mock(ProductResponse.class));

            // When
            productService.getProductById(id);
            productService.getProductById(id);
            productService.getProductById(id);

            // Then
            verify(productRepository, times(1)).findById(id);
        }

        @Test
        void getProductById_ShouldCacheSeparateEntries_ForDifferentIds() {
            // Given
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            Product productA = buildProduct(idA);
            Product productB = buildProduct(idB);

            ProductResponse responseA = mock(ProductResponse.class);
            ProductResponse responseB = mock(ProductResponse.class);

            when(productRepository.findById(idA)).thenReturn(Optional.of(productA));
            when(productRepository.findById(idB)).thenReturn(Optional.of(productB));
            when(productMapper.toProductResponse(productA)).thenReturn(responseA);
            when(productMapper.toProductResponse(productB)).thenReturn(responseB);

            // When
            productService.getProductById(idA);
            productService.getProductById(idB);

            // Then
            assertThat(getCachedValue(idA)).isEqualTo(responseA);
            assertThat(getCachedValue(idB)).isEqualTo(responseB);

            verify(productRepository, times(1)).findById(idA);
            verify(productRepository, times(1)).findById(idB);
        }

        @Test
        void getProductById_ShouldNotCacheResult_WhenProductNotFound() {
            // Given
            UUID id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.getProductById(id))
                    .isInstanceOf(ProductNotFoundException.class);
            assertThatThrownBy(() -> productService.getProductById(id))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, times(2)).findById(id);
        }
    }

    @Nested
    class DeleteProduct {
        @Test
        void deleteProduct_ShouldEvictCacheEntry_ForGivenId() {
            // Given
            UUID id = UUID.randomUUID();
            Product product = buildProduct(id);

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(mock(ProductResponse.class));

            productService.getProductById(id);
            assertThat(getProductsCache().get(id)).isNotNull();

            // When
            productService.deleteProduct(id);

            // Then
            assertThat(getProductsCache().get(id)).isNull();
        }

        @Test
        void deleteProduct_ShouldNotEvictOtherCachedEntries() {
            // Given
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            Product productA = buildProduct(idA);
            Product productB = buildProduct(idB);

            when(productRepository.findById(idA)).thenReturn(Optional.of(productA));
            when(productRepository.findById(idB)).thenReturn(Optional.of(productB));
            when(productMapper.toProductResponse(productA)).thenReturn(mock(ProductResponse.class));
            when(productMapper.toProductResponse(productB)).thenReturn(mock(ProductResponse.class));

            productService.getProductById(idA);
            productService.getProductById(idB);

            // When
            productService.deleteProduct(idA);

            // Then
            assertThat(getProductsCache().get(idA)).isNull();
            assertThat(getProductsCache().get(idB)).isNotNull();
        }
    }

    @Nested
    class UpdateStock {
        @Test
        void updateStock_ShouldPutResultInCache_ForGivenId() {
            // Given
            UUID id = UUID.randomUUID();
            Product product = buildProduct(id);
            ProductResponse response = mock(ProductResponse.class);

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(response);

            // When
            ProductResponse result = productService.adjustStock(id, 42);

            // Then
            assertThat(result).isEqualTo(response);
            assertThat(getCachedValue(id)).isEqualTo(response);
        }

        @Test
        void updateStock_ShouldOverwriteExistingCacheEntry() {
            // Given
            UUID id = UUID.randomUUID();
            Product product = buildProduct(id);

            ProductResponse oldResponse = mock(ProductResponse.class);
            ProductResponse newResponse = mock(ProductResponse.class);

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(oldResponse, newResponse);

            productService.getProductById(id);
            assertThat(getCachedValue(id)).isEqualTo(oldResponse);

            // When
            productService.adjustStock(id, 10);

            // Then
            assertThat(getCachedValue(id)).isEqualTo(newResponse);
        }

        @Test
        void updateStock_ShouldPopulateCache_SoSubsequentGetDoesNotHitRepository() {
            // Given
            UUID id = UUID.randomUUID();
            Product product = buildProduct(id);

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(mock(ProductResponse.class));

            // When
            productService.adjustStock(id, 5);
            productService.getProductById(id);

            // Then
            verify(productRepository, times(1)).findById(id);
        }
    }

    @Nested
    class UpdateProduct {
        @Test
        void updateProduct_ShouldPutResultInCache_ForGivenId() {
            // Given
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Product product = buildProduct(id);
            Category category = new Category();
            ProductResponse response = mock(ProductResponse.class);

            UpdateProductRequest request = mock(UpdateProductRequest.class);
            when(request.categoryId()).thenReturn(categoryId);

            when(categoryService.getCategoryById(categoryId)).thenReturn(category);
            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponse(product)).thenReturn(response);

            // When
            ProductResponse result = productService.updateProduct(id, request);

            // Then
            assertThat(result).isEqualTo(response);
            assertThat(getCachedValue(id)).isEqualTo(response);
        }

        @Test
        void updateProduct_ShouldNotWriteToCache_WhenCategoryDoesNotExist() {
            // Given
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            UpdateProductRequest request = mock(UpdateProductRequest.class);
            when(request.categoryId()).thenReturn(categoryId);
            when(categoryService.getCategoryById(categoryId))
                    .thenThrow(CategoryNotFoundException.class);
            when(productRepository.findById(id)).thenReturn(Optional.of(mock(Product.class)));

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct(id, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            assertThat(getProductsCache().get(id)).isNull();
            verifyNoInteractions(productMapper);
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_NAME);
        }

        @Bean
        ProductService productService(ProductRepository productRepository, CategoryService categoryService,
                                      ProductMapper productMapper) {
            return new ProductService(productRepository, categoryService, productMapper);
        }
    }
}