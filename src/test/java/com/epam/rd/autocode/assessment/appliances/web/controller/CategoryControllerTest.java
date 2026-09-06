package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

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

    @BeforeEach
    void setUp() {
        testAppliance = new ApplianceSummaryResponse(
                1L,
                "Refrigerator",
                "RB38",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(800.0)
        );
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should return index view with default parameters")
    void showApplianceCategoryPage_DefaultParams_ShouldReturnViewAndModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(
                List.of(testAppliance),
                PageRequest.of(0, 8, Sort.by("name").ascending()),
                1
        );

        when(applianceService.getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/category/BIG"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("appliances", "currentPage", "totalPages", "sortField", "sortDir", "reverseSortDir", "currentCategory", "baseUrl"))
                .andExpect(model().attribute("appliances", List.of(testAppliance)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("sortField", "name"))
                .andExpect(model().attribute("sortDir", "asc"))
                .andExpect(model().attribute("reverseSortDir", "desc"))
                .andExpect(model().attribute("currentCategory", "BIG"))
                .andExpect(model().attribute("baseUrl", "/category/BIG"));

        verify(applianceService, times(1)).getAllApplianceByCategory(
                eq(Category.BIG),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should handle lowercase category name in URL")
    void showApplianceCategoryPage_LowercaseCategory_ShouldParseEnumCorrectly() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/category/big"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentCategory", "BIG"))
                .andExpect(model().attribute("baseUrl", "/category/BIG"));

        verify(applianceService, times(1)).getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should redirect to home when category is invalid")
    void showApplianceCategoryPage_InvalidCategory_ShouldRedirectToHome() throws Exception {
        mockMvc.perform(get("/category/UNKNOWN_CATEGORY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(applianceService, never()).getAllApplianceByCategory(any(), any());
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should correctly configure descending sorting and reverseSortDir")
    void showApplianceCategoryPage_DescSorting_ShouldConfigureDescSort() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByCategory(eq(Category.SMALL), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/category/SMALL")
                        .param("sortField", "price")
                        .param("sortDir", "desc")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("sortField", "price"))
                .andExpect(model().attribute("sortDir", "desc"))
                .andExpect(model().attribute("reverseSortDir", "asc"))
                .andExpect(model().attribute("currentPage", 1));

        verify(applianceService, times(1)).getAllApplianceByCategory(
                eq(Category.SMALL),
                eq(PageRequest.of(1, 8, Sort.by("price").descending()))
        );
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should pass keyword to model when provided")
    void showApplianceCategoryPage_WithKeyword_ShouldAddKeywordToModel() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/category/BIG")
                        .param("keyword", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("keyword", "Samsung"));

        verify(applianceService, times(1)).getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /category/{categoryName} - Should normalize negative page number to 0")
    void showApplianceCategoryPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        Page<ApplianceSummaryResponse> pageResult = new PageImpl<>(List.of(testAppliance));

        when(applianceService.getAllApplianceByCategory(eq(Category.BIG), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/category/BIG")
                        .param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentPage", 0));

        verify(applianceService, times(1)).getAllApplianceByCategory(
                eq(Category.BIG),
                eq(PageRequest.of(0, 8, Sort.by("name").ascending()))
        );
    }
}