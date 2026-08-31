package com.epam.rd.autocode.assessment.appliances.dto.cart;

import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.model.Cart;

import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> cartItems
) {

    public static CartResponse fromEntity(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getItems().stream()
                        .map(CartItemResponse::fromEntity)
                        .toList()
        );
    }
}
