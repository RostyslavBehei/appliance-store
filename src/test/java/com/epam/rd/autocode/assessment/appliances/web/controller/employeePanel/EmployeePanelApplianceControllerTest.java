package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeePanelApplianceController.class)
class EmployeePanelApplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private ApplianceSummaryResponse testApplianceSummary;
    private ManufacturerResponse testManufacturer;

    @BeforeEach
    void setUp() {
        testApplianceSummary = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-500",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(199.99)
        );

        testManufacturer = new ManufacturerResponse(1L, "Samsung", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /employee-panel/appliances - Should redirect to login when unauthorized")
    void showAppliancesPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/appliances"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(applianceService, never()).getAllAppliance(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /employee-panel/appliances - Should return appliances page with default newest sort and pageable")
    void showAppliancesPage_DefaultParams_ShouldReturnViewAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(
                List.of(testApplianceSummary),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(applianceService.getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/appliances")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/appliance/employee-appliances-page"))
                .andExpect(model().attributeExists("appliances", "activePage"))
                .andExpect(model().attribute("appliances", pageResult))
                .andExpect(model().attribute("activePage", "appliances"));

        verify(applianceService, times(1)).getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/appliances - Should apply oldest sort, keyword, and custom pagination")
    void showAppliancesPage_CustomParams_ShouldPassToService() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testApplianceSummary));

        when(applianceService.getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/appliances")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "oldest")
                        .param("keyword", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/appliance/employee-appliances-page"))
                .andExpect(model().attribute("appliances", pageResult))
                .andExpect(model().attribute("activePage", "appliances"));

        verify(applianceService, times(1)).getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/appliances - Should normalize negative page to 0")
    void showAppliancesPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testApplianceSummary));

        when(applianceService.getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/appliances")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/appliance/employee-appliances-page"));

        verify(applianceService, times(1)).getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/appliances/new - Should open form with empty request and dropdown data")
    void showNewApplianceFormPage_ShouldReturnFormViewAndPopulateModel() throws Exception {
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/employee-panel/appliances/new")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/appliance/employee-appliance-form-page"))
                .andExpect(model().attributeExists("applianceRequest", "manufacturers", "categories", "powerTypes", "activePage"))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)))
                .andExpect(model().attribute("categories", Category.values()))
                .andExpect(model().attribute("powerTypes", PowerType.values()))
                .andExpect(model().attribute("activePage", "appliances"));

        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("POST /employee-panel/appliances/new - Should save appliance and redirect on valid input")
    void processNewAppliance_ValidRequest_ShouldSaveAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/appliances/new")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "Microwave Pro")
                        .param("category", "BIG")
                        .param("model", "MW-500")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "1200W")
                        .param("description", "Reliable microwave")
                        .param("power", "1200")
                        .param("price", "199.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/appliances"));

        verify(applianceService, times(1)).updateAppliance(any(ApplianceUpdateRequest.class));
    }

    @Test
    @DisplayName("POST /employee-panel/appliances/new - Should return form view when validation errors occur")
    void processNewAppliance_ValidationErrors_ShouldReturnFormView() throws Exception {
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(post("/employee-panel/appliances/new")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("price", "invalid-price")
                        .param("power", "not-a-number")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/appliance/employee-appliance-form-page"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("manufacturers", "categories", "powerTypes", "activePage"));

        verify(applianceService, never()).updateAppliance(any());
        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("POST /employee-panel/appliances/{id}/delete - Should delete appliance and redirect")
    void processDeleteAppliance_ShouldDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/appliances/10/delete")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/appliances"));

        verify(applianceService, times(1)).deleteApplianceById(10L);
    }
}