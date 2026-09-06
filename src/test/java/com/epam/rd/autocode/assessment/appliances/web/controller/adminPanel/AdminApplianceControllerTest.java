package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
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

@WebMvcTest(AdminApplianceController.class)
class AdminApplianceControllerTest {

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

    private ApplianceResponse testApplianceResponse;
    private ApplianceSummaryResponse testApplianceSummary;
    private ManufacturerResponse testManufacturer;

    @BeforeEach
    void setUp() {
        testApplianceResponse = new ApplianceResponse(
                1L,
                "Microwave",
                "BIG",
                "MW-300",
                "Samsung",
                "AC220",
                "1000W",
                "High quality microwave",
                1000,
                BigDecimal.valueOf(250.0)
        );

        testApplianceSummary = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-300",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(250.0)
        );

        testManufacturer = new ManufacturerResponse(1L, "Samsung", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /admin/appliances - Should return appliances page with default pagination and newest sort")
    void showAppliancePage_DefaultParams_ShouldReturnViewAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(
                List.of(testApplianceSummary),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(applianceService.getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(pageResult);

        mockMvc.perform(get("/admin/appliances")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-page"))
                .andExpect(model().attributeExists("activePage", "appliances"))
                .andExpect(model().attribute("activePage", "appliances"))
                .andExpect(model().attribute("appliances", pageResult))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(applianceService, times(1)).getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /admin/appliances - Should apply oldest sorting and keyword filter")
    void showAppliancePage_CustomSortAndKeyword_ShouldPassToService() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testApplianceSummary));

        when(applianceService.getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(pageResult);

        mockMvc.perform(get("/admin/appliances")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "oldest")
                        .param("keyword", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-page"))
                .andExpect(model().attribute("keyword", "Samsung"))
                .andExpect(model().attribute("appliances", pageResult));

        verify(applianceService, times(1)).getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /admin/appliances/{id} - Should return appliance details page")
    void showApplianceDetailPage_ShouldReturnViewAndAppliance() throws Exception {
        when(applianceService.getApplianceById(1L)).thenReturn(testApplianceResponse);

        mockMvc.perform(get("/admin/appliances/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-details-page"))
                .andExpect(model().attributeExists("activePage", "appliance"))
                .andExpect(model().attribute("activePage", "appliances"))
                .andExpect(model().attribute("appliance", testApplianceResponse));

        verify(applianceService, times(1)).getApplianceById(1L);
    }

    @Test
    @DisplayName("GET /admin/appliances/edit/{id} - Should return edit form with populated reference data")
    void showEditAppliancePage_ShouldReturnFormViewAndPopulateModel() throws Exception {
        when(applianceService.getApplianceById(1L)).thenReturn(testApplianceResponse);
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/admin/appliances/edit/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-form-page"))
                .andExpect(model().attributeExists("activePage", "manufacturers", "categories", "powerType", "appliance"))
                .andExpect(model().attribute("appliance", testApplianceResponse))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)));

        verify(applianceService, times(1)).getApplianceById(1L);
        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("GET /admin/appliances/new - Should return creation form with empty appliance")
    void showNewAppliancePage_ShouldReturnFormViewWithEmptyAppliance() throws Exception {
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/admin/appliances/new")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-form-page"))
                .andExpect(model().attributeExists("activePage", "manufacturers", "categories", "powerType", "appliance"))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)));

        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("POST /admin/appliances/save - Should update appliance and redirect on valid input")
    void processSaveAppliance_ValidRequest_ShouldSaveAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/appliances/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "1")
                        .param("name", "Microwave Oven")
                        .param("category", "BIG")
                        .param("model", "MW-300")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "1000W")
                        .param("description", "High quality microwave")
                        .param("power", "1000")
                        .param("price", "250.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appliances"));

        verify(applianceService, times(1)).updateAppliance(any(ApplianceUpdateRequest.class));
    }

    @Test
    @DisplayName("POST /admin/appliances/save - Should return form view when validation fails")
    void processSaveAppliance_ValidationErrors_ShouldReturnFormWithFormData() throws Exception {
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(post("/admin/appliances/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "not-a-number")
                        .param("name", "")
                        .param("price", "invalid-price"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appliance/admin-appliances-form-page"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("activePage", "manufacturers", "categories", "powerType"));

        verify(applianceService, never()).updateAppliance(any());
        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("POST /admin/appliances/delete/{id} - Should delete appliance and redirect to /admin/appliances")
    void processDeleteAppliance_ShouldDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/appliances/delete/1")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appliances"));

        verify(applianceService, times(1)).deleteApplianceById(1L);
    }
}