package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

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
            return "redirect:/login";
        }

        List<CartResponse> carts = cartService.getAllCarts(principal.getName());

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
    public String addCardItem(
            @Valid @ModelAttribute CartItemAddRequest request,
            Principal principal,
            HttpServletRequest req) {

        if (principal == null) {
            return "redirect:/login";
        }

        CartItemAddRequest newCardAppRequest = new CartItemAddRequest(
                principal.getName(),
                request.applianceId(),
                request.quantity()
        );

        cartService.addToCart(newCardAppRequest);

        return getReferer(req);
    }

    @PostMapping("/update")
    public String updateCartItemQuantity(
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") int quantity,
            HttpServletRequest req) {

        try {
            cartService.updateCartItemQuantity(cartItemId, quantity);
        } catch (Exception ex) {
            return "redirect:/";
        }
        return getReferer(req);
    }

    @PostMapping("/remove")
    public String removeCartItem(
            @RequestParam("cartItemId") Long cartItemId,
            HttpServletRequest req) {

        cartService.deleteCartItem(cartItemId);

        return getReferer(req);
    }

    private String getReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
