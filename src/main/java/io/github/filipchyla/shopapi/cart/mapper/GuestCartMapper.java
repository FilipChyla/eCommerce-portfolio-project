package io.github.filipchyla.shopapi.cart.mapper;

import io.github.filipchyla.shopapi.cart.PriceCalculator;
import io.github.filipchyla.shopapi.cart.domain.GuestCart;
import io.github.filipchyla.shopapi.cart.domain.GuestCartItem;
import io.github.filipchyla.shopapi.cart.dto.CartItemResponse;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.product.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = PriceCalculator.class)
public interface GuestCartMapper {
    @Mapping(target = "productId", source = "item.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "quantity", source = "item.quantity")
    @Mapping(target = "unitPriceSnapshot", source = "item.unitPriceSnapshot")
    @Mapping(target = "currentPrice", source = "product.price")
    @Mapping(target = "priceChanged", expression = "java(PriceCalculator.isPriceChanged(item.getUnitPriceSnapshot(), product.getPrice()))")
    @Mapping(target = "lineTotal", expression = "java(PriceCalculator.lineTotal(product.getPrice(), item.getQuantity()))")
    CartItemResponse toItemResponse(GuestCartItem item, Product product);

    default CartResponse toCartResponse(GuestCart cart, Map<UUID, Product> products) {
        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            Product product = products.get(item.getProductId());
            if (product == null) {
                return null;
            }
            return toItemResponse(item, product);
        }).filter(Objects::nonNull).toList();

        BigDecimal totalPrice = PriceCalculator.totalPrice(items);
        return new CartResponse(null, items, totalPrice);
    }
}