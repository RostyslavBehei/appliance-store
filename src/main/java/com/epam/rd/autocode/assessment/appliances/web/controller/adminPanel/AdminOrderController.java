package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
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

        int currentPage = Math.max(page, 0);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(currentPage, size, sorting);

        log.debug("Rendering admin orders: page={}, size={}, keyword='{}', sort='{}'",
                currentPage, size, keyword, sort);

        Page<OrderSummaryResponse> response = orderService.getAllOrdersSummary(keyword, pageable);
        model.addAttribute("ordersPage", response);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("activePage", "orders");

        return "admin/order/admin-orders-page";
    }

    @GetMapping("/{id}")
    public String showOrdersDetailPage(@PathVariable Long id, Model model) {
        log.debug("Viewing admin order details for id: {}", id);

        OrderResponse order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("activePage", "orders");

        return "admin/order/admin-orders-details-page";
    }

    @PostMapping("/{id}/approve")
    public String processOrderApprove(@PathVariable Long id, Principal principal) {
        String user = principal != null ? principal.getName() : "ADMIN";
        log.info("Admin/Employee '{}' approving order id: {}", user, id);

        orderService.approveOrder(id);
        log.info("Order id: {} approved successfully by '{}'", id, user);

        return "redirect:/admin/orders/" + id;
    }
}