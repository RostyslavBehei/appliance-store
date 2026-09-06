package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardEmployeeResponse;
import com.epam.rd.autocode.assessment.appliances.service.impl.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/employee-panel")
@RequiredArgsConstructor
public class EmployeePanelController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping
    public String showEmployeeAnalyticPage(Model model, Principal principal) {
        String employee = principal != null ? principal.getName() : "EMPLOYEE";
        log.debug("Employee '{}' accessed dashboard analytics", employee);

        DashboardEmployeeResponse response = dashboardService.getDashboardEmployeeResponse();

        model.addAttribute("dashboard", response);
        model.addAttribute("activePage", "dashboard");

        return "employee-panel/employee-dashboard-page";
    }
}