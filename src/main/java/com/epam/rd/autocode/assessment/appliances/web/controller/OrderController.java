package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public String showOrdersPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(0, 8);

        Page<OrderResponse> orders = orderService.getClientOrders(principal.getName(), pageable);

        model.addAttribute("orders", orders);
        return "order/orders-page";
    }

    @PostMapping("/checkout")
    public String processCartCheckout(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        orderService.checkout(principal.getName());

        return "redirect:/orders";
    }
}
