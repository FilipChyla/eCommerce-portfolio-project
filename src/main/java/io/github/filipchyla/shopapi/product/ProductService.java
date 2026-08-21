package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    public Page<ProductResponse> findProducts(UUID categoryId, BigDecimal minPrice,
                                              BigDecimal maxPrice, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must be smaller than maxPrice");
        }

        Specification<Product> spec = ProductSpecification.filterBy(categoryId, minPrice, maxPrice);
        return productRepository.findAll(spec, pageable).map(productMapper::toProductResponse);
    }

    public ProductResponse addProduct(CreateProductRequest newProduct) {
        Product product = new Product();
        product.setName(newProduct.name());
        product.setDescription(newProduct.description());
        product.setPrice(newProduct.price());
        product.setStockQuantity(newProduct.stockQuantity());

        Category category = categoryService.getCategoryById(newProduct.categoryId());

        product.setCategory(category);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(UUID id) {
        Product product = findProductById(id);
        product.setActive(false);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        Product product = findProductById(id);
        if (!product.isActive()) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        return productMapper.toProductResponse(product);
    }

    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateStock(UUID id, int quantity) {
        Product product = findProductById(id);
        product.setStockQuantity(quantity);

        return productMapper.toProductResponse(product);
    }

    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProduct(UUID id, @Valid UpdateProductRequest request) {
        Product product = findProductById(id);

        if (request.categoryId() != null){
            Category category = categoryService.getCategoryById(request.categoryId());
            product.setCategory(category);
        }

        productMapper.updateFromPatchRequest(request, product);
        return productMapper.toProductResponse(product);
    }

    private Product findProductById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }
}
