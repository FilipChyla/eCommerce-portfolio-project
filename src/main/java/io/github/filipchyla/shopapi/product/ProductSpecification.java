package io.github.filipchyla.shopapi.product;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecification {
    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null :
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceGreaterOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null :
                cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null :
                cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> filterBy(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return Specification.allOf(
                hasCategory(categoryId),
                priceGreaterOrEqual(minPrice),
                priceLessOrEqual(maxPrice),
                isActive()
        );
    }
}
