package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardAdminResponse;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping
    public String showAdminPage(Model model, Principal principal) {
        String adminEmail = principal != null ? principal.getName() : "system";
        log.debug("Admin '{}' accessed admin dashboard", adminEmail);

        DashboardAdminResponse response = dashboardService.getDashboardAdminResponse();
        model.addAttribute("dashboard", response);
        model.addAttribute("activePage", "dashboard");

        return "admin/admin-dashboard-page";
    }
}