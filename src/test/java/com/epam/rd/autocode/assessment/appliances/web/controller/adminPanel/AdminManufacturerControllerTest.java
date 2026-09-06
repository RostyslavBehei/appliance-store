package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
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

@WebMvcTest(AdminManufacturerController.class)
class AdminManufacturerControllerTest {

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
    @DisplayName("GET /admin/manufacturers - Should redirect when unauthorized")
    void showManufacturerPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/manufacturers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(manufacturerService, never()).getAllManufacturer(any());
    }

    @Test
    @DisplayName("GET /admin/manufacturers - Should return manufacturers list view")
    void showManufacturerPage_WithoutKeyword_ShouldReturnList() throws Exception {
        when(manufacturerService.getAllManufacturer(null)).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/admin/manufacturers")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-page"))
                .andExpect(model().attributeExists("manufacturersList", "activePage"))
                .andExpect(model().attribute("manufacturersList", List.of(testManufacturer)))
                .andExpect(model().attribute("activePage", "manufacturers"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(manufacturerService, times(1)).getAllManufacturer(null);
    }

    @Test
    @DisplayName("GET /admin/manufacturers - Should return filtered manufacturers when keyword provided")
    void showManufacturerPage_WithKeyword_ShouldPassKeywordToService() throws Exception {
        when(manufacturerService.getAllManufacturer("Bosch")).thenReturn(List.of(testManufacturer));

        mockMvc.perform(get("/admin/manufacturers")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("keyword", "Bosch"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-page"))
                .andExpect(model().attribute("keyword", "Bosch"))
                .andExpect(model().attribute("manufacturersList", List.of(testManufacturer)))
                .andExpect(model().attribute("activePage", "manufacturers"));

        verify(manufacturerService, times(1)).getAllManufacturer("Bosch");
    }

    @Test
    @DisplayName("GET /admin/manufacturers/new - Should display creation form with empty manufacturer")
    void showNewManufacturerPage_ShouldReturnFormView() throws Exception {
        mockMvc.perform(get("/admin/manufacturers/new")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-form-page"))
                .andExpect(model().attributeExists("manufacturer", "activePage"))
                .andExpect(model().attribute("activePage", "manufacturers"));
    }

    @Test
    @DisplayName("GET /admin/manufacturers/edit/{id} - Should return edit form with existing manufacturer")
    void showEditManufacturerPage_ShouldReturnFormView() throws Exception {
        when(manufacturerService.getManufacturerById(1L)).thenReturn(testManufacturer);

        mockMvc.perform(get("/admin/manufacturers/edit/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-form-page"))
                .andExpect(model().attributeExists("manufacturer", "activePage"))
                .andExpect(model().attribute("manufacturer", testManufacturer))
                .andExpect(model().attribute("activePage", "manufacturers"));

        verify(manufacturerService, times(1)).getManufacturerById(1L);
    }

    @Test
    @DisplayName("POST /admin/manufacturers/save - Should update existing manufacturer when id is present")
    void processEditManufacturer_WithId_ShouldCallUpdateServiceAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/manufacturers/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "1")
                        .param("name", "Bosch Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/manufacturers"));

        verify(manufacturerService, times(1)).updateManufacturer(any(ManufacturerUpdateRequest.class));
        verify(manufacturerService, never()).createManufacturer(any());
    }

    @Test
    @DisplayName("POST /admin/manufacturers/save - Should create new manufacturer when id is null")
    void processEditManufacturer_WithoutId_ShouldCallCreateServiceAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/manufacturers/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Philips"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/manufacturers"));

        verify(manufacturerService, times(1)).createManufacturer(eq(new ManufacturerCreateRequest("Philips")));
        verify(manufacturerService, never()).updateManufacturer(any());
    }

    @Test
    @DisplayName("POST /admin/manufacturers/save - Should return form view with errors on binding validation failure")
    void processEditManufacturer_ValidationErrors_ShouldReturnFormView() throws Exception {
        mockMvc.perform(post("/admin/manufacturers/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("id", "not-a-number")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-form-page"))
                .andExpect(model().hasErrors())
                .andExpect(model().attribute("activePage", "manufacturers"));

        verify(manufacturerService, never()).createManufacturer(any());
        verify(manufacturerService, never()).updateManufacturer(any());
    }

    @Test
    @DisplayName("POST /admin/manufacturers/save - Should return form view with errorMessage when AlreadyExistsException thrown")
    void processEditManufacturer_AlreadyExistsException_ShouldReturnFormWithError() throws Exception {
        doThrow(new AlreadyExistsException("Manufacturer with this name already exists"))
                .when(manufacturerService).createManufacturer(any(ManufacturerCreateRequest.class));

        mockMvc.perform(post("/admin/manufacturers/save")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "ExistingName"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/manufacturer/admin-manufacturers-form-page"))
                .andExpect(model().attributeExists("errorMessage", "activePage"))
                .andExpect(model().attribute("errorMessage", "Manufacturer with this name already exists"))
                .andExpect(model().attribute("activePage", "manufacturers"));

        verify(manufacturerService, times(1)).createManufacturer(any(ManufacturerCreateRequest.class));
    }

    @Test
    @DisplayName("POST /admin/manufacturers/delete/{id} - Should delete manufacturer and redirect")
    void processDeleteManufacturer_ShouldDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/manufacturers/delete/1")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/manufacturers"));

        verify(manufacturerService, times(1)).deleteManufacturerById(1L);
    }
}