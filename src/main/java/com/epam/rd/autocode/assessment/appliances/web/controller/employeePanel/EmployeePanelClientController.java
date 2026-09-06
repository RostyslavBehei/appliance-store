package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/employee-panel/clients")
@RequiredArgsConstructor
public class EmployeePanelClientController {

    private final OrderService orderService;
    private final ClientService clientService;

    @GetMapping
    public String showClientsPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Model model) {

        int currentPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(currentPage, size, Sort.by(Sort.Direction.ASC, "lastName"));

        log.debug("Employee fetching clients page: page={}, size={}, keyword='{}'", currentPage, size, keyword);
        Page<ClientSummaryResponse> clients = clientService.getClientsPage(keyword, pageable);

        model.addAttribute("clients", clients);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "clients");

        return "employee-panel/client/employee-clients-page";
    }

    @GetMapping("/{id}")
    public String showClientDetailsPage(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Model model) {

        int currentPage = Math.max(page, 0);
        log.debug("Employee viewing client details for client id: {}", id);

        Pageable pageable = PageRequest.of(currentPage, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        ClientProfileResponse client = clientService.getClientById(id);
        Page<OrderResponse> orders = orderService.getClientOrders(client.email(), pageable);

        model.addAttribute("client", client);
        model.addAttribute("orders", orders);
        model.addAttribute("size", size);
        model.addAttribute("activePage", "clients");

        return "employee-panel/client/employee-client-details-page";
    }
}