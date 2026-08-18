package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.category.CategoryNotFoundException;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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
    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();

        product = new Product();
        product.setId(productId);
        product.setName("Existing product");
        product.setDescription("Existing description");
        product.setPrice(BigDecimal.valueOf(50));
        product.setStockQuantity(10);
        product.setCategory(category);
    }

    @Nested
    class FindProducts {
        @Test
        void findProducts_ShouldReturnPage_WhenFiltersAreValid() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> expectedPage = new PageImpl<>(List.of(product));

            when(productRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(expectedPage);

            Page<Product> result = productService.findProducts(
                    categoryId, BigDecimal.TEN, BigDecimal.valueOf(100), pageable);

            assertThat(result).isEqualTo(expectedPage);
            verify(productRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        void findProducts_ShouldReturnPage_WhenPriceBoundsAreNull() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> expectedPage = new PageImpl<>(List.of(product));

            when(productRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(expectedPage);

            Page<Product> result = productService.findProducts(null, null, null, pageable);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void findProducts_ShouldThrowIllegalArgumentException_WhenMinPriceGreaterThanMaxPrice() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> productService.findProducts(
                    categoryId, BigDecimal.valueOf(100), BigDecimal.TEN, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minPrice must be smaller than maxPrice");

            verifyNoInteractions(productRepository);
        }
    }

    @Nested
    class AddProduct {
        @Test
        void addProduct_ShouldSaveProduct_WhenCategoryExists() {
            CreateProductRequest request = new CreateProductRequest(
                    "New product", "New description", BigDecimal.valueOf(20), 5, categoryId);

            when(categoryService.getCategoryById(categoryId)).thenReturn(category);
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Product result = productService.addProduct(request);

            assertThat(result.getName()).isEqualTo("New product");
            assertThat(result.getDescription()).isEqualTo("New description");
            assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(20));
            assertThat(result.getStockQuantity()).isEqualTo(5);
            assertThat(result.getCategory()).isEqualTo(category);

            verify(productRepository).save(any(Product.class));
        }

        @Test
        void addProduct_ShouldThrowNotFound_WhenCategoryDoesNotExist() {
            CreateProductRequest request = new CreateProductRequest(
                    "New product", "New description", BigDecimal.valueOf(20), 5, categoryId);

            when(categoryService.getCategoryById(categoryId))
                    .thenThrow(new CategoryNotFoundException("Category not found with id: " + categoryId));

            assertThatThrownBy(() -> productService.addProduct(request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProduct {
        @Test
        void deleteProduct_ShouldMarkProductInactive_WhenProductExists() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            productService.deleteProduct(productId);

            assertThat(product.isActive()).isFalse();
        }

        @Test
        void deleteProduct_ShouldThrowNotFound_WhenProductDoesNotExist() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found with id: " + productId);
        }
    }

    @Nested
    class GetProductById {
        @Test
        void getProductById_ShouldReturnProduct_WhenProductExists() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Product result = productService.getProductById(productId);

            assertThat(result).isEqualTo(product);
        }

        @Test
        void getProductById_ShouldThrowNotFound_WhenProductDoesNotExist() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found with id: " + productId);
        }
    }

    @Nested
    class UpdateStock {
        @Test
        void updateStock_ShouldUpdateStockQuantity_WhenProductExists() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Product result = productService.updateStock(productId, 42);

            assertThat(result.getStockQuantity()).isEqualTo(42);
        }

        @Test
        void updateStock_ShouldThrowNotFound_WhenProductDoesNotExist() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateStock(productId, 42))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    class UpdateProduct {
        @Test
        void updateProduct_ShouldUpdateCategoryAndApplyPatch_WhenProductAndCategoryExist() {
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = new Category();
            UpdateProductRequest request = new UpdateProductRequest(
                    "Updated name", "Updated description", BigDecimal.valueOf(30), 7, newCategoryId);

            when(categoryService.getCategoryById(newCategoryId)).thenReturn(newCategory);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Product result = productService.updateProduct(productId, request);

            assertThat(result.getCategory()).isEqualTo(newCategory);
            verify(productMapper).updateFromPatchRequest(request, product);
        }

        @Test
        void updateProduct_ShouldThrowNotFound_WhenCategoryDoesNotExist() {
            UUID newCategoryId = UUID.randomUUID();
            UpdateProductRequest request = new UpdateProductRequest(
                    "Updated name", "Updated description", BigDecimal.valueOf(30), 7, newCategoryId);

            when(categoryService.getCategoryById(newCategoryId))
                    .thenThrow(new CategoryNotFoundException("Category not found with id: " + newCategoryId));

            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verifyNoInteractions(productRepository, productMapper);
        }

        @Test
        void updateProduct_ShouldThrowNotFound_WhenProductDoesNotExist() {
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = new Category();
            UpdateProductRequest request = new UpdateProductRequest(
                    "Updated name", "Updated description", BigDecimal.valueOf(30), 7, newCategoryId);

            when(categoryService.getCategoryById(newCategoryId)).thenReturn(newCategory);
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                    .isInstanceOf(ProductNotFoundException.class);

            verifyNoInteractions(productMapper);
        }
    }
}