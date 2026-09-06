package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManufacturerController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManufacturerControllerTest {

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
    private final String brandName = "Samsung";

    @BeforeEach
    void setUp() {
        testAppliance = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-300",
                "BIG",
                brandName,
                BigDecimal.valueOf(180.0)
        );
    }

    @Test
    @DisplayName("GET /brand/{brandName} - Should return brand page with default pagination and attributes")
    void showApplianceManufacturerPage_DefaultParams_ShouldReturnViewAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(
                List.of(testAppliance),
                PageRequest.of(0, 8, Sort.by("name").ascending()),
                1
        );

        when(applianceService.getAllApplianceByManufacturerName(eq(brandName), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/brand/{brandName}", brandName))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("appliances", "currentPage", "totalPages", "sortField", "sortDir", "reverseSortDir", "currentBrand", "baseUrl"))
                .andExpect(model().attribute("appliances", List.of(testAppliance)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("sortField", "name"))
                .andExpect(model().attribute("sortDir", "asc"))
                .andExpect(model().attribute("reverseSortDir", "desc"))
                .andExpect(model().attribute("currentBrand", brandName))
                .andExpect(model().attribute("baseUrl", "/brand/" + brandName))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(applianceService, times(1)).getAllApplianceByManufacturerName(
                eq(brandName),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
    }

    @Test
    @DisplayName("GET /brand/{brandName} - Should configure descending sort and reverseSortDir correctly")
    void showApplianceManufacturerPage_DescSort_ShouldConfigureDescSort() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByManufacturerName(eq(brandName), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/brand/{brandName}", brandName)
                        .param("sortField", "price")
                        .param("sortDir", "desc")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("sortField", "price"))
                .andExpect(model().attribute("sortDir", "desc"))
                .andExpect(model().attribute("reverseSortDir", "asc"))
                .andExpect(model().attribute("currentPage", 1));

        verify(applianceService, times(1)).getAllApplianceByManufacturerName(
                eq(brandName),
                eq(PageRequest.of(1, 8, Sort.by("price").descending()))
        );
    }

    @Test
    @DisplayName("GET /brand/{brandName} - Should pass keyword to model when provided")
    void showApplianceManufacturerPage_WithKeyword_ShouldAddKeywordToModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByManufacturerName(eq(brandName), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/brand/{brandName}", brandName)
                        .param("keyword", "Microwave"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("keyword", "Microwave"));

        verify(applianceService, times(1)).getAllApplianceByManufacturerName(eq(brandName), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /brand/{brandName} - Should normalize negative page to 0")
    void showApplianceManufacturerPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByManufacturerName(eq(brandName), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/brand/{brandName}", brandName)
                        .param("page", "-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentPage", 0));

        verify(applianceService, times(1)).getAllApplianceByManufacturerName(
                eq(brandName),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
    }

    @Test
    @DisplayName("GET /brand/{brandName} - Should handle empty results gracefully")
    void showApplianceManufacturerPage_EmptyResults_ShouldReturnZeroTotalPages() throws Exception {
        Page<ApplianceSummaryResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 8), 0);

        when(applianceService.getAllApplianceByManufacturerName(eq("UnknownBrand"), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/brand/{brandName}", "UnknownBrand"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("appliances", Collections.emptyList()))
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("currentBrand", "UnknownBrand"))
                .andExpect(model().attribute("baseUrl", "/brand/UnknownBrand"));

        verify(applianceService, times(1)).getAllApplianceByManufacturerName(eq("UnknownBrand"), any(Pageable.class));
    }
}