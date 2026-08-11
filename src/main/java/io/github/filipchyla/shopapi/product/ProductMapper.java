package io.github.filipchyla.shopapi.product;

import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(source = "category.name", target = "categoryName")
    ProductResponse toProductResponse(Product product);
}
