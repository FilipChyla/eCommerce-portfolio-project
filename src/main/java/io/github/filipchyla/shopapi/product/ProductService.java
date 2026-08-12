package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.product.dto.CreateProductRequest;
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
        productRepository.deleteById(id);
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }
}
