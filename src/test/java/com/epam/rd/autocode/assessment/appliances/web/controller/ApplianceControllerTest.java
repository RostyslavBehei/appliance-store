package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplianceController.class)
class ApplianceControllerTest {

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
    private ManufacturerResponse testManufacturerResponse;

    @BeforeEach
    void setUp() {
        testApplianceResponse = new ApplianceResponse(
                1L,
                "Microwave",
                String.valueOf(Category.BIG),
                "Model X",
                "Samsung",
                String.valueOf(PowerType.AC220),
                "Features",
                "Desc",
                800,
                BigDecimal.valueOf(150.0)
        );

        testManufacturerResponse = new ManufacturerResponse(1L, "Samsung", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /appliances/{id} - Should return appliance details page")
    void showAppliancePage_ShouldReturnViewAndModel() throws Exception {
        when(applianceService.getApplianceById(1L)).thenReturn(testApplianceResponse);

        mockMvc.perform(get("/appliances/1")
                        .with(user("guest").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/appliance-details"))
                .andExpect(model().attributeExists("appliance"))
                .andExpect(model().attribute("appliance", testApplianceResponse));

        verify(applianceService, times(1)).getApplianceById(1L);
    }

    @Test
    @DisplayName("GET /appliances/edit/{id} - Should return edit page if user authenticated")
    void showEditAppliancePage_WithPrincipal_ShouldReturnView() throws Exception {
        when(applianceService.getApplianceById(1L)).thenReturn(testApplianceResponse);
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturerResponse));

        mockMvc.perform(get("/appliances/edit/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/appliance-edit-page"))
                .andExpect(model().attributeExists("appliance", "manufacturers", "categories", "powerTypes"));

        verify(applianceService, times(1)).getApplianceById(1L);
        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("GET /appliances/edit/{id} - Should redirect when user not authenticated")
    void showEditAppliancePage_WithoutPrincipal_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/appliances/edit/1"))
                .andExpect(status().is3xxRedirection());

        verify(applianceService, never()).getApplianceById(anyLong());
    }

    @Test
    @DisplayName("POST /appliances/edit - Should redirect when user not authenticated")
    void processEditAppliance_WithoutPrincipal_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/appliances/edit")
                        .with(csrf())
                        .param("id", "1")
                        .param("name", "Updated Microwave"))
                .andExpect(status().is3xxRedirection());

        verify(applianceService, never()).updateAppliance(any());
    }

    @Test
    @DisplayName("POST /appliances/edit - Should return edit form view when validation fails")
    void processEditAppliance_WithValidationErrors_ShouldReturnFormView() throws Exception {
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturerResponse));

        mockMvc.perform(post("/appliances/edit")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "1")
                        .param("name", "")
                        .param("price", "-50.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/appliance-edit-page"))
                .andExpect(model().attributeExists("appliance", "manufacturers", "categories", "powerTypes"));

        verify(applianceService, never()).updateAppliance(any());
    }

    @Test
    @DisplayName("POST /appliances/edit - Should process edit and redirect to details page")
    void processEditAppliance_WithPrincipal_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/appliances/edit")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "1")
                        .param("name", "Updated Microwave")
                        .param("category", "BIG")
                        .param("manufacturerName", "Samsung")
                        .param("price", "200.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances/1"));

        verify(applianceService, times(1)).updateAppliance(any(ApplianceUpdateRequest.class));
    }

    @Test
    @DisplayName("POST /appliances/delete/{id} - Should redirect when user not authenticated")
    void deleteAppliance_WithoutPrincipal_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/appliances/delete/1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(applianceService, never()).deleteApplianceById(anyLong());
    }

    @Test
    @DisplayName("POST /appliances/delete/{id} - Should delete and redirect to home")
    void deleteAppliance_WithPrincipal_ShouldRedirectToHome() throws Exception {
        mockMvc.perform(post("/appliances/delete/1")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(applianceService, times(1)).deleteApplianceById(1L);
    }
}