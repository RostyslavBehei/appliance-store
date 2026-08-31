package com.epam.rd.autocode.assessment.appliances.web.controller.admin;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardAdminResponse;
import com.epam.rd.autocode.assessment.appliances.service.impl.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DashboardServiceImpl dashboardService;

    @GetMapping
    public String showAdminPage(Model model) {
        DashboardAdminResponse response = dashboardService.getDashboardAdminResponse();
        model.addAttribute("dashboard", response);
        model.addAttribute("activePage", "dashboard");
        return "admin/admin-dashboard-page";
    }

}
