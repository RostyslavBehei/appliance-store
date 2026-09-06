package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeePanelManufacturerController.class)
class EmployeePanelManufacturerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManufacturerService manufacturerService;

    @MockBean
    private ApplianceService applianceService;

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

    private ManufacturerResponse testManufacturer;

    @BeforeEach
    void setUp() {
        testManufacturer = new ManufacturerResponse(1L, "Bosch", LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /employee-panel/manufacturers - Should redirect to login when unauthorized")
    void showManufacturersPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/manufacturers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(manufacturerService, never()).getAllManufacturer(any());
    }

    @Test
    @DisplayName("GET /employee-panel/manufacturers - Should return manufacturers list view")
    void showManufacturersPage_WithoutKeyword_ShouldReturnList() throws Exception {
        when(manufacturerService.getAllManufacturer(null)).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/employee-panel/manufacturers")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/manufacturer/employee-manufacturer-page"))
                .andExpect(model().attributeExists("manufacturers", "activePage"))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)))
                .andExpect(model().attribute("activePage", "manufacturers"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(manufacturerService, times(1)).getAllManufacturer(null);
    }

    @Test
    @DisplayName("GET /employee-panel/manufacturers - Should pass keyword to service when provided")
    void showManufacturersPage_WithKeyword_ShouldReturnFilteredList() throws Exception {
        when(manufacturerService.getAllManufacturer("Bosch")).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/employee-panel/manufacturers")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("keyword", "Bosch"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/manufacturer/employee-manufacturer-page"))
                .andExpect(model().attribute("keyword", "Bosch"))
                .andExpect(model().attribute("manufacturers", List.of(testManufacturer)))
                .andExpect(model().attribute("activePage", "manufacturers"));

        verify(manufacturerService, times(1)).getAllManufacturer("Bosch");
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers - Should create manufacturer and redirect on valid name")
    void processAddManufacturer_ValidName_ShouldCreateAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/manufacturers")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "  LG Electronics  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"));

        verify(manufacturerService, times(1))
                .createManufacturer(eq(new ManufacturerCreateRequest("LG Electronics")));
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers - Should add flash error and not call service on empty name")
    void processAddManufacturer_BlankName_ShouldRedirectWithFlashError() throws Exception {
        mockMvc.perform(post("/employee-panel/manufacturers")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"))
                .andExpect(flash().attribute("errorMessage", "Manufacturer name cannot be empty"));

        verify(manufacturerService, never()).createManufacturer(any());
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers - Should handle service exception with flash error")
    void processAddManufacturer_ServiceException_ShouldRedirectWithFlashError() throws Exception {
        doThrow(new RuntimeException("Manufacturer already exists"))
                .when(manufacturerService).createManufacturer(any(ManufacturerCreateRequest.class));

        mockMvc.perform(post("/employee-panel/manufacturers")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "DuplicateName"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"))
                .andExpect(flash().attribute("errorMessage", "Manufacturer already exists"));

        verify(manufacturerService, times(1))
                .createManufacturer(eq(new ManufacturerCreateRequest("DuplicateName")));
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers/{id}/edit - Should update manufacturer on valid name")
    void processEditManufacturer_ValidName_ShouldUpdateAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/manufacturers/1/edit")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "  Bosch Pro  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"));

        verify(manufacturerService, times(1))
                .updateManufacturer(eq(new ManufacturerUpdateRequest(1L, "Bosch Pro")));
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers/{id}/edit - Should add flash error and not call service on blank name")
    void processEditManufacturer_BlankName_ShouldRedirectWithFlashError() throws Exception {
        mockMvc.perform(post("/employee-panel/manufacturers/1/edit")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"))
                .andExpect(flash().attribute("errorMessage", "Manufacturer name cannot be empty"));

        verify(manufacturerService, never()).updateManufacturer(any());
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers/{id}/edit - Should handle service exception with flash error")
    void processEditManufacturer_ServiceException_ShouldRedirectWithFlashError() throws Exception {
        doThrow(new RuntimeException("Update failed"))
                .when(manufacturerService).updateManufacturer(any(ManufacturerUpdateRequest.class));

        mockMvc.perform(post("/employee-panel/manufacturers/1/edit")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("name", "Valid Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"))
                .andExpect(flash().attribute("errorMessage", "Update failed"));

        verify(manufacturerService, times(1))
                .updateManufacturer(eq(new ManufacturerUpdateRequest(1L, "Valid Name")));
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers/{id}/delete - Should delete manufacturer and redirect")
    void deleteManufacturer_Success_ShouldDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/manufacturers/1/delete")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"));

        verify(manufacturerService, times(1)).deleteManufacturerById(1L);
    }

    @Test
    @DisplayName("POST /employee-panel/manufacturers/{id}/delete - Should add custom flash error when linked to appliances")
    void deleteManufacturer_OnException_ShouldRedirectWithCustomFlashError() throws Exception {
        doThrow(new RuntimeException("Foreign key constraint violation"))
                .when(manufacturerService).deleteManufacturerById(1L);

        mockMvc.perform(post("/employee-panel/manufacturers/1/delete")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/manufacturers"))
                .andExpect(flash().attribute("errorMessage", "Cannot delete manufacturer: it may be linked to existing appliances."));

        verify(manufacturerService, times(1)).deleteManufacturerById(1L);
    }
}