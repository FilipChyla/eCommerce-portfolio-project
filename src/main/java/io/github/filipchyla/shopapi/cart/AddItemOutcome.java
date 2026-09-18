package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.CartResponse;

public sealed interface AddItemOutcome {
    record Added(CartResponse cart) implements AddItemOutcome {}
    record Rejected(RuntimeException cause) implements AddItemOutcome {}
}
