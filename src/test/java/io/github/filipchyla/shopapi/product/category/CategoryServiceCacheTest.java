package io.github.filipchyla.shopapi.product.category;

import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.SingleCategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CategoryServiceCacheTest.CacheTestConfig.class)
class CategoryServiceCacheTest {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private CategoryMapper categoryMapper;

    private static final String CACHE_NAME = "categories";
    private static final String CACHE_KEY = "tree";

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        cache.clear();

        when(categoryRepository.findAllOrdered()).thenReturn(List.of());
    }

    private Cache getCategoriesCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        return cache;
    }

    @Nested
    class GetCategoryTree {
        @Test
        void getCategoryTree_ShouldCallRepositoryOnlyOnce_WhenCalledMultipleTimes() {
            // When
            categoryService.getCategoryTree();
            categoryService.getCategoryTree();
            categoryService.getCategoryTree();

            // Then
            verify(categoryRepository, times(1)).findAllOrdered();
        }

        @Test
        void getCategoryTree_ShouldStoreResultUnderExpectedCacheKey() {
            // When
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            // Then
            Cache.ValueWrapper cachedValue = getCategoriesCache().get(CACHE_KEY);

            assertThat(cachedValue).isNotNull();
            assertThat(cachedValue.get()).isEqualTo(result);
        }

        @Test
        void getCategoryTree_ShouldHitRepositoryAgain_AfterCacheIsManuallyCleared() {
            // Given
            categoryService.getCategoryTree();

            // When
            getCategoriesCache().clear();
            categoryService.getCategoryTree();

            // Then
            verify(categoryRepository, times(2)).findAllOrdered();
        }
    }

    @Nested
    class AddCategory {
        @Test
        void addCategory_ShouldEvictTreeCache() {
            // Given
            categoryService.getCategoryTree();
            verify(categoryRepository, times(1)).findAllOrdered();

            Category savedCategory = new Category();
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
            when(categoryMapper.toSingleCategoryResponse(savedCategory))
                    .thenReturn(mock(SingleCategoryResponse.class));

            CreateCategoryRequest request = mock(CreateCategoryRequest.class);
            when(request.categoryName()).thenReturn("Electronics");
            when(request.parentId()).thenReturn(null);

            // When
            categoryService.addCategory(request);

            // Then
            assertThat(getCategoriesCache().get(CACHE_KEY)).isNull();

            categoryService.getCategoryTree();
            verify(categoryRepository, times(2)).findAllOrdered();
        }
    }

    @Nested
    class DeleteCategory {
        @Test
        void deleteCategory_ShouldEvictTreeCache() {
            // Given
            when(categoryRepository.existsById(any())).thenReturn(true);
            categoryService.getCategoryTree();
            verify(categoryRepository, times(1)).findAllOrdered();

            // When
            categoryService.deleteCategory(UUID.randomUUID());

            // Then
            assertThat(getCategoriesCache().get(CACHE_KEY)).isNull();

            categoryService.getCategoryTree();
            verify(categoryRepository, times(2)).findAllOrdered();
        }

        @Test
        void deleteCategory_ShouldThrowAndNotEvictCache_WhenCategoryDoesNotExist() {
            // Given
            categoryService.getCategoryTree();

            UUID id = UUID.randomUUID();
            when(categoryRepository.existsById(id)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(id))
                    .isInstanceOf(CategoryNotFoundException.class);

            assertThat(getCategoriesCache().get(CACHE_KEY)).isNotNull();
            verify(categoryRepository, never()).deleteById(any());
        }
    }

    @Nested
    class UpdateCategory {
        @Test
        void updateCategory_ShouldEvictTreeCache() {
            // Given
            categoryService.getCategoryTree();
            verify(categoryRepository, times(1)).findAllOrdered();

            UUID id = UUID.randomUUID();
            Category existing = new Category();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryMapper.toSingleCategoryResponse(existing))
                    .thenReturn(mock(SingleCategoryResponse.class));

            UpdateCategoryRequest request = mock(UpdateCategoryRequest.class);
            when(request.parentId()).thenReturn(null);

            // When
            categoryService.updateCategory(id, request);

            // Then
            assertThat(getCategoriesCache().get(CACHE_KEY)).isNull();

            categoryService.getCategoryTree();
            verify(categoryRepository, times(2)).findAllOrdered();
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
        CategoryService categoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
            return new CategoryService(categoryRepository, categoryMapper);
        }
    }
}