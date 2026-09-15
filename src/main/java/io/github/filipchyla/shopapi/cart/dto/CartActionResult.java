package io.github.filipchyla.shopapi.cart.dto;

public record CartActionResult(CartResponse cart, String cartTokenToSet) {
}
