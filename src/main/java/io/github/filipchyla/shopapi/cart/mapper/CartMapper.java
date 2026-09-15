package io.github.filipchyla.shopapi.cart.mapper;

import io.github.filipchyla.shopapi.cart.domain.Cart;
import io.github.filipchyla.shopapi.cart.domain.CartItem;
import io.github.filipchyla.shopapi.cart.dto.CartItemResponse;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.PriceCalculator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = PriceCalculator.class)
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unitPriceSnapshot", source = "unitPriceSnapshot")
    @Mapping(target = "currentPrice", source = "product.price")
    @Mapping(target = "priceChanged", expression = "java(PriceCalculator.isPriceChanged(item.getUnitPriceSnapshot(), item.getProduct().getPrice()))")
    @Mapping(target = "lineTotal", expression = "java(PriceCalculator.lineTotal(item.getProduct().getPrice(), item.getQuantity()))")
    CartItemResponse toCartItemResponse(CartItem item);

    @Mapping(target = "totalPrice", expression = "java(PriceCalculator.totalPrice(items))")
    CartResponse toCartResponse(Cart cart);
}
