package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryNotFoundException;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Product productEntity;
    private ProductResponse productResponse;
    private Category category;

    private final String PRODUCT_NAME = "New product";
    private final String PRODUCT_DESCRIPTION = "New product";
    private final BigDecimal PRODUCT_PRICE = BigDecimal.valueOf(20);
    private final int PRODUCT_QUANTITY = 20;
    private final String CATEGORY_NAME = "Category";

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName(CATEGORY_NAME);

        productEntity = new Product();
        productEntity.setId(productId);
        productEntity.setName(PRODUCT_NAME);
        productEntity.setDescription(PRODUCT_DESCRIPTION);
        productEntity.setPrice(PRODUCT_PRICE);
        productEntity.setStockQuantity(PRODUCT_QUANTITY);
        productEntity.setCategory(category);
        productEntity.setActive(true);

        productResponse = new ProductResponse(
                productId,
                PRODUCT_NAME,
                PRODUCT_DESCRIPTION,
                PRODUCT_PRICE,
                PRODUCT_QUANTITY,
                CATEGORY_NAME,
                Instant.now()
        );
    }

    @Nested
    class FindProducts {
        @Test
        void findProducts_ShouldReturnPage_WhenFiltersAreValid() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> products = new PageImpl<>(List.of(productEntity));

            when(productRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(products);
            when(productMapper.toProductResponse(productEntity))
                    .thenReturn(productResponse);

            // When
            Page<ProductResponse> result = productService.findProducts(
                    categoryId,
                    BigDecimal.TEN,
                    BigDecimal.valueOf(100),
                    pageable
            );

            // Then
            assertThat(result.getContent()).containsExactly(productResponse);

            verify(productRepository).findAll(any(Specification.class), eq(pageable));
            verify(productMapper).toProductResponse(productEntity);
        }

        @Test
        void findProducts_ShouldReturnPage_WhenPriceBoundsAreNull() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> products = new PageImpl<>(List.of(productEntity));

            when(productRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(products);
            when(productMapper.toProductResponse(productEntity))
                    .thenReturn(productResponse);

            // When
            Page<ProductResponse> result = productService.findProducts(
                    null,
                    null,
                    null,
                    pageable
            );

            // Then
            assertThat(result.getContent()).containsExactly(productResponse);

            verify(productRepository).findAll(any(Specification.class), eq(pageable));
            verify(productMapper).toProductResponse(productEntity);
        }

        @Test
        void findProducts_ShouldThrowIllegalArgumentException_WhenMinPriceGreaterThanMaxPrice() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When & Then
            assertThatThrownBy(() -> productService.findProducts(
                    categoryId,
                    BigDecimal.valueOf(100),
                    BigDecimal.TEN,
                    pageable
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minPrice must be smaller than maxPrice");

            verifyNoInteractions(productRepository, productMapper);
        }
    }

    @Nested
    class AddProduct {
        private CreateProductRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateProductRequest(
                    PRODUCT_NAME,
                    PRODUCT_DESCRIPTION,
                    PRODUCT_PRICE,
                    PRODUCT_QUANTITY,
                    categoryId
            );
        }

        @Test
        void addProduct_ShouldSaveProduct_WhenCategoryExists() {
            // Given
            when(categoryService.getCategoryById(categoryId))
                    .thenReturn(category);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(productMapper.toProductResponse(any(Product.class)))
                    .thenReturn(productResponse);

            // When
            ProductResponse result = productService.addProduct(request);

            // Then
            assertThat(result).isEqualTo(productResponse);

            verify(categoryService).getCategoryById(categoryId);
            verify(productRepository).save(any(Product.class));
            verify(productMapper).toProductResponse(any(Product.class));
        }

        @Test
        void addProduct_ShouldThrowNotFound_WhenCategoryDoesNotExist() {
            // Given
            when(categoryService.getCategoryById(categoryId))
                    .thenThrow(new CategoryNotFoundException(
                            "Category not found with id: " + categoryId
                    ));

            // When & Then
            assertThatThrownBy(() -> productService.addProduct(request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(productRepository, never()).save(any());
            verifyNoInteractions(productMapper);
        }
    }

    @Nested
    class DeleteProduct {
        @Test
        void deleteProduct_ShouldMarkProductInactive_WhenProductExists() {
            // Given
            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(productEntity));

            // When
            productService.deleteProduct(productId);

            // Then
            assertThat(productEntity.isActive()).isFalse();

            verify(productRepository).findById(productId);
        }

        @Test
        void deleteProduct_ShouldThrowNotFound_WhenProductDoesNotExist() {
            // Given
            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.deleteProduct(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found with id: " + productId);

            verify(productRepository).findById(productId);
        }
    }

    @Nested
    class GetProductById {
        @Test
        void getProductById_ShouldReturnProductResponse_WhenProductExists() {
            // Given
            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(productEntity));
            when(productMapper.toProductResponse(productEntity))
                    .thenReturn(productResponse);

            // When
            ProductResponse result = productService.getProductById(productId);

            // Then
            assertThat(result).isEqualTo(productResponse);

            verify(productRepository).findById(productId);
            verify(productMapper).toProductResponse(productEntity);
        }

        @Test
        void getProductById_ShouldThrowNotFound_WhenProductDoesNotExist() {
            // Given
            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.getProductById(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found with id: " + productId);

            verify(productMapper, never()).toProductResponse(any());
        }
    }

    @Nested
    class UpdateStock {
        @Test
        void updateStock_ShouldUpdateStockQuantity_WhenProductExists() {
            // Given
            ProductResponse expectedResponse = new ProductResponse(
                    productId,
                    PRODUCT_NAME,
                    PRODUCT_DESCRIPTION,
                    PRODUCT_PRICE,
                    42,
                    CATEGORY_NAME,
                    productResponse.createdAt()
            );

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(productEntity));
            when(productMapper.toProductResponse(productEntity))
                    .thenReturn(expectedResponse);

            // When
            ProductResponse result = productService.updateStock(productId, 42);

            // Then
            assertThat(productEntity.getStockQuantity()).isEqualTo(42);
            assertThat(result).isEqualTo(expectedResponse);

            verify(productRepository).findById(productId);
            verify(productMapper).toProductResponse(productEntity);
        }

        @Test
        void updateStock_ShouldThrowNotFound_WhenProductDoesNotExist() {
            // Given
            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.updateStock(productId, 42))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productMapper, never()).toProductResponse(any());
        }
    }

    @Nested
    class UpdateProduct {
        UpdateProductRequest request;
        UUID newCategoryId = UUID.randomUUID();
        Category newCategory = new Category();

        @BeforeEach
        void setUp() {
            request = new UpdateProductRequest(
                    "Updated name",
                    "Updated description",
                    BigDecimal.valueOf(30),
                    7,
                    newCategoryId
            );
        }

        @Test
        void updateProduct_ShouldUpdateCategoryAndApplyPatch_WhenProductAndCategoryExist() {
            // Given
            when(categoryService.getCategoryById(newCategoryId))
                    .thenReturn(newCategory);
            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(productEntity));
            when(productMapper.toProductResponse(productEntity))
                    .thenReturn(productResponse);

            // When
            ProductResponse result = productService.updateProduct(productId, request);

            // Then
            assertThat(productEntity.getCategory()).isEqualTo(newCategory);
            assertThat(result).isEqualTo(productResponse);

            verify(categoryService).getCategoryById(newCategoryId);
            verify(productRepository).findById(productId);
            verify(productMapper).updateFromPatchRequest(request, productEntity);
            verify(productMapper).toProductResponse(productEntity);
        }

        @Test
        void updateProduct_ShouldThrowNotFound_WhenCategoryDoesNotExist() {
            // Given
            when(categoryService.getCategoryById(newCategoryId))
                    .thenThrow(new CategoryNotFoundException(
                            "Category not found with id: " + newCategoryId
                    ));

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(productRepository, never()).findById(any());
            verifyNoInteractions(productMapper);
        }

        @Test
        void updateProduct_ShouldThrowNotFound_WhenProductDoesNotExist() {
            // Given
            when(categoryService.getCategoryById(newCategoryId))
                    .thenReturn(newCategory);
            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found with id: " + productId);

            verify(productRepository).findById(productId);
            verifyNoInteractions(productMapper);
        }
    }
}