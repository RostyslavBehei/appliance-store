package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderCheckoutRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping
    public String showOrdersPage(Model model, Principal principal) {
        if (principal == null) {
            log.warn("Unauthorized access attempt to /orders page");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.debug("Rendering orders list page for user: '{}'", email);

        Pageable pageable = PageRequest.of(0, 8);
        Page<OrderResponse> orders = orderService.getClientOrders(email, pageable);

        model.addAttribute("orders", orders);
        return "order/orders-page";
    }

    @GetMapping("/checkout")
    public String showOrderCheckoutPage(Model model, Principal principal) {
        if (principal == null) {
            log.warn("Unauthorized access attempt to /orders/checkout");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.debug("Rendering checkout page for user: '{}'", email);

        populateCheckoutModel(model, email);

        if (!model.containsAttribute("checkoutRequest")) {
            model.addAttribute("checkoutRequest",
                    new OrderCheckoutRequest("", "", "", "", "UA", "", "", "", "CASH"));
        }

        return "order/order-checkout-page";
    }

    @GetMapping("/{id}")
    public String showOrderDetailsPage(Model model, Principal principal, @PathVariable Long id) {
        if (principal == null) {
            log.warn("Unauthorized access attempt to view order id: {}", id);
            return "redirect:/login";
        }

        log.debug("User '{}' viewing details for order id: {}", principal.getName(), id);
        OrderResponse response = orderService.getOrderById(id);

        model.addAttribute("order", response);
        return "order/order-details-page";
    }

    @PostMapping
    public String processCartCheckout(@Valid @ModelAttribute("checkoutRequest") OrderCheckoutRequest request,
                                      BindingResult bindingResult,
                                      Model model,
                                      Principal principal) {
        if (principal == null) {
            log.warn("Unauthorized submission to process checkout");
            return "redirect:/login";
        }

        String email = principal.getName();

        if (bindingResult.hasErrors()) {
            log.warn("Order checkout validation failed for user '{}': {} error(s)", email, bindingResult.getErrorCount());
            populateCheckoutModel(model, email);
            return "order/order-checkout-page";
        }

        try {
            log.info("Initiating checkout submission for user: '{}', payment: '{}'", email, request.paymentMethod());
            orderService.checkout(email, request);
            log.info("Checkout successfully completed for user: '{}'", email);
            return "redirect:/orders";
        } catch (IllegalStateException ex) {
            log.warn("Checkout rejected for user '{}': {}", email, ex.getMessage());
            populateCheckoutModel(model, email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "order/order-checkout-page";
        } catch (Exception ex) {
            log.error("Unexpected error during checkout for user '{}': {}", email, ex.getMessage(), ex);
            populateCheckoutModel(model, email);
            model.addAttribute("errorMessage", "An error occurred while placing your order. Please try again.");
            return "order/order-checkout-page";
        }
    }

    private void populateCheckoutModel(Model model, String email) {
        List<CartResponse> carts = cartService.getAllCarts(email);
        int totalItems = cartService.getCartItemCount(email);

        BigDecimal totalSum = carts.stream()
                .filter(cart -> cart.cartItems() != null)
                .flatMap(cart -> cart.cartItems().stream())
                .map(CartItemResponse::sum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("carts", carts);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalSum", totalSum);
    }
}