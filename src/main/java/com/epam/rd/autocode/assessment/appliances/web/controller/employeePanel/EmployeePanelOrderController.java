package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

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
@RequestMapping("/employee-panel/orders")
@RequiredArgsConstructor
public class EmployeePanelOrderController {

    private final OrderService orderService;

    @GetMapping
    public String showOrdersPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size,
            Model model
    ) {
        int currentPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(currentPage, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderSummaryResponse> response = orderService.getAllOrdersSummary(keyword, pageable);

        model.addAttribute("orders", response);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "orders");

        return "employee-panel/order/employee-orders-page";
    }

    @GetMapping("/{id}")
    public String showOrdersDetailsPage(@PathVariable Long id, Model model) {
        log.debug("Employee viewing details for order id: {}", id);

        OrderResponse order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("activePage", "orders");

        return "employee-panel/order/employee-order-details-page";
    }

    @PostMapping("/{id}/approve")
    public String processApproveOrder(@PathVariable Long id, Principal principal) {
        String employee = principal != null ? principal.getName() : "EMPLOYEE";
        log.info("Employee '{}' approving order id: {}", employee, id);

        orderService.approveOrder(id);
        log.info("Order id: {} approved successfully by employee '{}'", id, employee);

        return "redirect:/employee-panel/orders";
    }
}