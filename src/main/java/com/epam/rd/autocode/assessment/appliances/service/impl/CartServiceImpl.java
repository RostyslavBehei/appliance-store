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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ApplianceRepository applianceRepository;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public void addToCart(CartItemAddRequest request) {
        log.info("Adding appliance id={} (qty={}) to cart for user='{}'",
                request.applianceId(), request.quantity(), request.clientEmail());

        User user = userRepository.findByEmail(request.clientEmail())
                .orElseThrow(() -> {
                    log.warn("Failed to add to cart: User with email '{}' not found", request.clientEmail());
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{request.clientEmail()}, getLocale())
                    );
                });

        if (!(user instanceof Client client)) {
            log.warn("Failed to add to cart: User '{}' is not a CLIENT", request.clientEmail());
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.only.client.cart", null, getLocale())
            );
        }

        Cart cart = cartRepository.findByClient(client).orElseGet(() -> {
            log.info("Cart not found for client '{}', creating a new cart", client.getEmail());
            Cart newCart = Cart.builder()
                    .client(client)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });

        Appliance appliance = applianceRepository.findById(request.applianceId())
                .orElseThrow(() -> {
                    log.warn("Failed to add to cart: Appliance with id {} not found", request.applianceId());
                    return new NotFoundException(
                            messageSource.getMessage("error.appliance.not.found", new Object[]{request.applianceId()}, getLocale())
                    );
                });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getAppliance() != null && item.getAppliance().getId().equals(request.applianceId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.quantity();
            log.info("Appliance id={} already in cart. Updating quantity: {} -> {}",
                    request.applianceId(), item.getQuantity(), newQuantity);
            item.setQuantity(newQuantity);
        } else {
            CartItem newCartItem = CartItem.builder()
                    .cart(cart)
                    .appliance(appliance)
                    .quantity(request.quantity())
                    .build();

            cart.getItems().add(newCartItem);
            log.info("Appliance id={} added as a new cart item", request.applianceId());
        }

        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponse> getAllCarts(String clientEmail) {
        log.debug("Fetching carts for user: '{}'", clientEmail);

        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> {
                    log.warn("Failed to fetch carts: User '{}' not found", clientEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{clientEmail}, getLocale())
                    );
                });

        if (!(user instanceof Client client)) {
            log.warn("User '{}' is not a CLIENT. Returning empty cart list", clientEmail);
            return List.of();
        }

        return cartRepository.findByClient(client).stream()
                .map(CartResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(String clientEmail) {
        log.debug("Fetching cart item count for user: '{}'", clientEmail);
        Optional<User> userOpt = userRepository.findByEmail(clientEmail);

        if (userOpt.isEmpty() || !(userOpt.get() instanceof Client client)) {
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
        int finalQuantity = Math.max(newQuantity, 1);
        log.info("Updating quantity for cartItem id={} to {}", cartItemId, finalQuantity);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.warn("Failed to update quantity: CartItem id={} not found", cartItemId);
                    return new NotFoundException(
                            messageSource.getMessage("error.cart.item.not.found", new Object[]{cartItemId}, getLocale())
                    );
                });

        cartItem.setQuantity(finalQuantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void deleteCartItem(Long cartItemId) {
        log.info("Attempting to delete cartItem id={}", cartItemId);

        if (!cartItemRepository.existsById(cartItemId)) {
            log.warn("Failed to delete: CartItem id={} not found", cartItemId);
            throw new NotFoundException(
                    messageSource.getMessage("error.cart.item.not.found", new Object[]{cartItemId}, getLocale())
            );
        }

        cartItemRepository.forceDeleteById(cartItemId);
        log.info("CartItem id={} successfully deleted", cartItemId);
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}