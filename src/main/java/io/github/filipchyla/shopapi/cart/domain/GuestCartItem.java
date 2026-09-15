package io.github.filipchyla.shopapi.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestCartItem {
    private UUID productId;
    private int quantity;
    private BigDecimal unitPriceSnapshot;
}
