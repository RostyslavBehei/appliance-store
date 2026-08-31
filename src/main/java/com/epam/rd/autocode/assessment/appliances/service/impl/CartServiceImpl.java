package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.model.Cart;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.CartItemRepository;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ApplianceRepository applianceRepository;

    @Override
    @Transactional(readOnly = true)
    public void addToCart(CartItemAddRequest request) {
        User user = userRepository.findByEmail(request.clientEmail())
                .orElseThrow(() -> new NotFoundException("User with email " + request.clientEmail() + " not found"));

        if (!(user instanceof Client client)) {
            throw new IllegalArgumentException("Only client can add to Cart");
        }

        Cart Cart = cartRepository.findByClient(client).orElseGet(() -> {
            Cart newCart = com.epam.rd.autocode.assessment.appliances.model.Cart.builder().client(client).build();
            return cartRepository.save(newCart);
        });

        Appliance appliance = applianceRepository.findById(request.applianceId())
                .orElseThrow(() -> new NotFoundException("Appliance with id " + request.applianceId() + " not found"));

        Optional<CartItem> existingItem = Cart.getItems().stream()
                .filter(item -> item.getId().equals(request.applianceId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(request.quantity());
        } else {
            CartItem newCartItem = CartItem.builder()
                    .cart(Cart)
                    .appliance(appliance)
                    .quantity(request.quantity())
                    .build();

            Cart.getItems().add(newCartItem);
        }
        cartRepository.save(Cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponse> getAllCarts(String clientEmail) {
        Client client = (Client) userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + clientEmail + " not found"));

        return cartRepository.findByClient(client).stream()
                .map(CartResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(String clientEmail) {
        Client client = (Client) userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + clientEmail + " not found"));

        if (client == null) {
            return 0;
        }

        return cartRepository.findByClient(client)
                .map(Cart -> {
                    if (Cart.getItems() == null) {
                        return 0;
                    }

                    return Cart.getItems().stream()
                        .mapToInt(CartItem::getQuantity)
                        .sum();
                })
                .orElse(0);
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long cartItemId, int newQuantity) {
        if (newQuantity < 1) newQuantity = 1;
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("Item with id " + cartItemId + " not found"));

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void deleteCartItem(Long cartItemId) {
        cartItemRepository.forceDeleteById(cartItemId);
    }
}
