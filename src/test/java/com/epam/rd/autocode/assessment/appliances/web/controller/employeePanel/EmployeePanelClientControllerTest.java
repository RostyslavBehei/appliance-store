package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeePanelClientController.class)
class EmployeePanelClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ClientService clientService;

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
    @DisplayName("GET /employee-panel/clients - Should redirect to login when unauthorized")
    void showClientsPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/clients"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(clientService, never()).getClientsPage(any(), any());
    }

    @Test
    @DisplayName("GET /employee-panel/clients - Should return clients page with default pagination and sorting by lastName ASC")
    void showClientsPage_DefaultParams_ShouldReturnClientsView() throws Exception {
        ClientSummaryResponse summary = mock(ClientSummaryResponse.class);
        Page<ClientSummaryResponse> pageResult = new PageImpl<>(
                List.of(summary),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")),
                1
        );

        when(clientService.getClientsPage(isNull(), eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")))))
                .thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/clients")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/client/employee-clients-page"))
                .andExpect(model().attributeExists("clients", "activePage"))
                .andExpect(model().attribute("clients", pageResult))
                .andExpect(model().attribute("activePage", "clients"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(clientService, times(1)).getClientsPage(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/clients - Should apply custom pagination and keyword")
    void showClientsPage_CustomParams_ShouldPassToService() throws Exception {
        ClientSummaryResponse summary = mock(ClientSummaryResponse.class);
        Page<ClientSummaryResponse> pageResult = new PageImpl<>(List.of(summary));
        Pageable expectedPageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "lastName"));

        when(clientService.getClientsPage(eq("John"), eq(expectedPageable))).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/clients")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "2")
                        .param("size", "5")
                        .param("keyword", "John"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/client/employee-clients-page"))
                .andExpect(model().attribute("clients", pageResult))
                .andExpect(model().attribute("keyword", "John"))
                .andExpect(model().attribute("activePage", "clients"));

        verify(clientService, times(1)).getClientsPage(eq("John"), eq(expectedPageable));
    }

    @Test
    @DisplayName("GET /employee-panel/clients - Should normalize negative page parameter to 0")
    void showClientsPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        ClientSummaryResponse summary = mock(ClientSummaryResponse.class);
        Page<ClientSummaryResponse> pageResult = new PageImpl<>(List.of(summary));

        when(clientService.getClientsPage(isNull(), eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")))))
                .thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/clients")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/client/employee-clients-page"));

        verify(clientService, times(1)).getClientsPage(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/clients/{id} - Should redirect when unauthorized")
    void showClientDetailsPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/clients/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(clientService, never()).getClientById(anyLong());
        verify(orderService, never()).getClientOrders(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /employee-panel/clients/{id} - Should return client details view with client and orders")
    void showClientDetailsPage_WithEmployeeUser_ShouldReturnDetailsView() throws Exception {
        String clientEmail = "client@test.com";
        ClientProfileResponse clientResponse = mock(ClientProfileResponse.class);
        when(clientResponse.email()).thenReturn(clientEmail);

        OrderResponse orderResponse = mock(OrderResponse.class);
        Pageable expectedOrdersPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<OrderResponse> ordersPage = new PageImpl<>(List.of(orderResponse), expectedOrdersPageable, 1);

        when(clientService.getClientById(10L)).thenReturn(clientResponse);
        when(orderService.getClientOrders(clientEmail, expectedOrdersPageable)).thenReturn(ordersPage);

        mockMvc.perform(get("/employee-panel/clients/10")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/client/employee-client-details-page"))
                .andExpect(model().attributeExists("client", "orders", "activePage"))
                .andExpect(model().attribute("client", clientResponse))
                .andExpect(model().attribute("orders", ordersPage))
                .andExpect(model().attribute("activePage", "clients"));

        verify(clientService, times(1)).getClientById(10L);
        verify(orderService, times(1)).getClientOrders(clientEmail, expectedOrdersPageable);
    }

    @Test
    @DisplayName("GET /employee-panel/clients/{id} - Should normalize negative page to 0 when loading orders")
    void showClientDetailsPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        String clientEmail = "client@test.com";
        ClientProfileResponse clientResponse = mock(ClientProfileResponse.class);
        when(clientResponse.email()).thenReturn(clientEmail);

        Pageable expectedOrdersPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<OrderResponse> ordersPage = new PageImpl<>(List.of());

        when(clientService.getClientById(10L)).thenReturn(clientResponse);
        when(orderService.getClientOrders(clientEmail, expectedOrdersPageable)).thenReturn(ordersPage);

        mockMvc.perform(get("/employee-panel/clients/10")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "-2"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/client/employee-client-details-page"));

        verify(orderService, times(1)).getClientOrders(clientEmail, expectedOrdersPageable);
    }
}