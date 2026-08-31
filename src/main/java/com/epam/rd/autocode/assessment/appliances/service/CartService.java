package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CartService {
    void addToCart(CartItemAddRequest request);
    int getCartItemCount(String clientEmail);
    List<CartResponse> getAllCarts(String clientEmail);

    void updateCartItemQuantity(Long cartItemId, int newQuantity);
    void deleteCartItem(Long cartItemId);
}
