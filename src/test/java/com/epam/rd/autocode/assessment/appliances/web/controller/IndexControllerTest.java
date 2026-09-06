package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
@AutoConfigureMockMvc(addFilters = false)
class IndexControllerTest {

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

    private ApplianceSummaryResponse testAppliance;
    private ManufacturerResponse testManufacturer;

    @BeforeEach
    void setUp() {
        testAppliance = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-100",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(150.0)
        );

        testManufacturer = new ManufacturerResponse(1L, "Samsung", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET / - Should return home page with default pagination and attributes")
    void showHomePage_DefaultParams_ShouldReturnViewAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(
                List.of(testAppliance),
                PageRequest.of(0, 8, Sort.by("name").ascending()),
                1
        );

        when(applianceService.getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn(pageResult);
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("appliances", "manufacturers", "currentPage", "totalPages", "sortField", "sortDir", "reverseSortDir", "baseUrl"))
                .andExpect(model().attribute("appliances", List.of(testAppliance)))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("sortField", "name"))
                .andExpect(model().attribute("sortDir", "asc"))
                .andExpect(model().attribute("reverseSortDir", "desc"))
                .andExpect(model().attribute("baseUrl", "/"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(applianceService, times(1)).getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
        verify(manufacturerService, times(1)).getAllManufacturer();
    }

    @Test
    @DisplayName("GET / - Should support search keyword, custom sorting and custom page")
    void showHomePage_CustomParams_ShouldPassToServiceAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn(pageResult);
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/")
                        .param("keyword", "Samsung")
                        .param("page", "2")
                        .param("sortField", "price")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("keyword", "Samsung"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("sortField", "price"))
                .andExpect(model().attribute("sortDir", "desc"))
                .andExpect(model().attribute("reverseSortDir", "asc"));

        verify(applianceService, times(1)).getAllAppliance(
                eq("Samsung"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(2, 8, Sort.by("price").descending()))
        );
    }

    @Test
    @DisplayName("GET / - Should normalize negative page parameter to zero")
    void showHomePage_NegativePage_ShouldNormalizeToZero() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn(pageResult);
        when(manufacturerService.getAllManufacturer()).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/").param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentPage", 0));

        verify(applianceService, times(1)).getAllAppliance(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
    }

    @Test
    @DisplayName("GET /about - Should return about page with activePage attribute")
    void showAboutPage_ShouldReturnAboutView() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about-us"))
                .andExpect(model().attribute("activePage", "about"));
    }

    @Test
    @DisplayName("GET /contact - Should return contact page with activePage attribute")
    void showContactPage_ShouldReturnContactView() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("contact"))
                .andExpect(model().attribute("activePage", "contact"));
    }

    @Test
    @DisplayName("POST /contact - Should process contact form and redirect to /contact")
    void processContactPage_ShouldRedirectToContact() throws Exception {
        mockMvc.perform(post("/contact").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"));
    }
}