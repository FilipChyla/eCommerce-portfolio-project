package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
import io.github.filipchyla.shopapi.product.dto.UpdateProductRequest;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    public Page<Product> findProducts(UUID categoryId, BigDecimal minPrice,
                                              BigDecimal maxPrice, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must be smaller than maxPrice");
        }

        Specification<Product> spec = ProductSpecification.filterBy(categoryId, minPrice, maxPrice);

        return productRepository.findAll(spec, pageable);
    }
    public Product addProduct(CreateProductRequest newProduct) {
        Product product = new Product();
        product.setName(newProduct.name());
        product.setDescription(newProduct.description());
        product.setPrice(newProduct.price());
        product.setStockQuantity(newProduct.stockQuantity());

        Category category = categoryService.getCategoryById(newProduct.categoryId());

        product.setCategory(category);

        return productRepository.save(product);
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setActive(false);
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product updateStock(UUID id, int quantity) {
        Product product = getProductById(id);
        product.setStockQuantity(quantity);

        return product;
    }

    @Transactional
    public Product updateProduct(UUID id, @Valid UpdateProductRequest request) {
        Category category = categoryService.getCategoryById(request.categoryId());

        Product product = getProductById(id);

        product.setCategory(category);
        productMapper.updateFromPatchRequest(request, product);
        return product;
    }
}
