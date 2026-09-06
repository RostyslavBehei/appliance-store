package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String showCartPage(
            Principal principal,
            Model model) {

        if (principal == null) {
            log.warn("Unauthorized attempt to access cart page");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.debug("Rendering cart page for user: '{}'", email);

        List<CartResponse> carts = cartService.getAllCarts(email);

        BigDecimal totalSum = carts.stream()
                .filter(cart -> cart.cartItems() != null)
                .flatMap(cart -> cart.cartItems().stream())
                .map(CartItemResponse::sum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalItems = carts.stream()
                .filter(cart -> cart.cartItems() != null)
                .mapToLong(cart -> cart.cartItems().size())
                .sum();

        model.addAttribute("carts", carts);
        model.addAttribute("totalSum", totalSum);
        model.addAttribute("totalItems", totalItems);

        return "cart/cart-page";
    }

    @PostMapping("/add")
    public String addCartItem(
            @Valid @ModelAttribute CartItemAddRequest request,
            BindingResult bindingResult,
            Principal principal,
            HttpServletRequest req) {

        if (principal == null) {
            log.warn("Unauthorized attempt to add appliance id={} to cart", request.applianceId());
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            log.warn("Failed to add to cart: validation errors for user '{}'", principal.getName());
            return getSafeReferer(req);
        }

        String email = principal.getName();
        log.info("User '{}' adding appliance id={} (qty={}) to cart",
                email, request.applianceId(), request.quantity());

        CartItemAddRequest newCartRequest = new CartItemAddRequest(
                email,
                request.applianceId(),
                request.quantity()
        );

        cartService.addToCart(newCartRequest);

        return getSafeReferer(req);
    }

    @PostMapping("/update")
    public String updateCartItemQuantity(
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            HttpServletRequest req) {

        String user = (principal != null) ? principal.getName() : "anonymous";

        try {
            log.info("User '{}' updating quantity for cartItem id={} to {}", user, cartItemId, quantity);
            cartService.updateCartItemQuantity(cartItemId, quantity);
        } catch (Exception ex) {
            log.error("Failed to update cartItem id={} for user '{}': {}", cartItemId, user, ex.getMessage(), ex);
            return "redirect:/cart?error=true";
        }

        return getSafeReferer(req);
    }

    @PostMapping("/remove")
    public String removeCartItem(
            @RequestParam("cartItemId") Long cartItemId,
            Principal principal,
            HttpServletRequest req) {

        String user = (principal != null) ? principal.getName() : "anonymous";
        log.info("User '{}' removing cartItem id={}", user, cartItemId);

        cartService.deleteCartItem(cartItemId);

        return getSafeReferer(req);
    }

    private String getSafeReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer != null && (referer.startsWith("/") || referer.contains(req.getServerName()))) {
            return "redirect:" + referer;
        }
        return "redirect:/cart";
    }
}