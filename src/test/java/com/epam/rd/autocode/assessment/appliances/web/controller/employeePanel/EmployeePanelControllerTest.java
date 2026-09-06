package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardEmployeeResponse;
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

@WebMvcTest(EmployeePanelController.class)
class EmployeePanelControllerTest {

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
    @DisplayName("GET /employee-panel - Should return employee dashboard view and model attributes for authenticated EMPLOYEE")
    void showEmployeeAnalyticPage_WithEmployeeUser_ShouldReturnDashboardView() throws Exception {
        DashboardEmployeeResponse mockResponse = mock(DashboardEmployeeResponse.class);
        when(dashboardService.getDashboardEmployeeResponse()).thenReturn(mockResponse);

        mockMvc.perform(get("/employee-panel")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/employee-dashboard-page"))
                .andExpect(model().attributeExists("dashboard", "activePage"))
                .andExpect(model().attribute("dashboard", mockResponse))
                .andExpect(model().attribute("activePage", "dashboard"));

        verify(dashboardService, times(1)).getDashboardEmployeeResponse();
    }

    @Test
    @DisplayName("GET /employee-panel - Should redirect to login/OAuth2 endpoint when unauthorized")
    void showEmployeeAnalyticPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(dashboardService, never()).getDashboardEmployeeResponse();
    }
}