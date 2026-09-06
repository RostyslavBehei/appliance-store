package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardAdminResponse;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import com.epam.rd.autocode.assessment.appliances.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardServiceImpl dashboardService;

    @MockBean
    private ApplianceService applianceService;

    @MockBean
    private ManufacturerService manufacturerService;

    @MockBean
    private CartService cartService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private JwtCore jwtCore;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /admin - Should return dashboard view for authenticated ADMIN")
    void showAdminPage_WithAdminUser_ShouldReturnDashboardView() throws Exception {
        DashboardAdminResponse mockResponse = mock(DashboardAdminResponse.class);
        when(dashboardService.getDashboardAdminResponse()).thenReturn(mockResponse);

        mockMvc.perform(get("/admin")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-dashboard-page"))
                .andExpect(model().attributeExists("dashboard", "activePage"))
                .andExpect(model().attribute("dashboard", mockResponse))
                .andExpect(model().attribute("activePage", "dashboard"));

        verify(dashboardService, times(1)).getDashboardAdminResponse();
    }

    @Test
    @DisplayName("GET /admin - Should redirect unauthorized requests to login")
    void showAdminPage_WithoutAuthentication_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());

        verify(dashboardService, never()).getDashboardAdminResponse();
    }
}