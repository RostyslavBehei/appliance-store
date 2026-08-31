package com.epam.rd.autocode.assessment.appliances.web.controller.admin;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String showOrdersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        Sort sorting = sort.equals("oldest") ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<OrderSummaryResponse> response = orderService.getAllOrdersSummary(keyword, pageable);
        model.addAttribute("ordersPage", response);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("activePage", "orders");

        return "admin/order/admin-orders-page";
    }

    @GetMapping("/{id}")
    public String showOrdersDetailPage(
            @PathVariable Long id,
            Model model
    ) {

        OrderResponse order = orderService.getOrderById(id);

        model.addAttribute("order", order);
        model.addAttribute("activePage", "orders");


        return "admin/order/admin-orders-details-page";
    }

    @PostMapping("/{id}/approve")
    public String processOrderApprove(@PathVariable Long id) {
        orderService.approveOrder(id);

        return "redirect:/admin/orders/" + id;
    }
}
