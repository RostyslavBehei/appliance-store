package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeePanelOrderController.class)
class EmployeePanelOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

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
    @DisplayName("GET /employee-panel/orders - Should redirect to login when unauthorized")
    void showOrdersPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).getAllOrdersSummary(any(), any());
    }

    @Test
    @DisplayName("GET /employee-panel/orders - Should return orders list with default pagination and createdAt DESC sort")
    void showOrdersPage_DefaultParams_ShouldReturnOrdersView() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(
                List.of(summary),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(orderService.getAllOrdersSummary(isNull(), any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/orders")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/order/employee-orders-page"))
                .andExpect(model().attributeExists("orders", "activePage"))
                .andExpect(model().attribute("orders", pageResult))
                .andExpect(model().attribute("activePage", "orders"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(orderService, times(1)).getAllOrdersSummary(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/orders - Should apply custom pagination and keyword")
    void showOrdersPage_CustomParams_ShouldPassToService() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(List.of(summary));

        when(orderService.getAllOrdersSummary(eq("ORD-123"), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/orders")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "2")
                        .param("size", "5")
                        .param("keyword", "ORD-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/order/employee-orders-page"))
                .andExpect(model().attribute("orders", pageResult))
                .andExpect(model().attribute("keyword", "ORD-123"))
                .andExpect(model().attribute("activePage", "orders"));

        verify(orderService, times(1)).getAllOrdersSummary(
                eq("ORD-123"),
                eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/orders - Should normalize negative page to 0")
    void showOrdersPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(List.of(summary));

        when(orderService.getAllOrdersSummary(isNull(), any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/employee-panel/orders")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/order/employee-orders-page"));

        verify(orderService, times(1)).getAllOrdersSummary(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /employee-panel/orders/{id} - Should redirect when unauthorized")
    void showOrdersDetailsPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/employee-panel/orders/50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).getOrderById(anyLong());
    }

    @Test
    @DisplayName("GET /employee-panel/orders/{id} - Should return order details page")
    void showOrdersDetailsPage_WithEmployeeUser_ShouldReturnDetailsView() throws Exception {
        OrderResponse orderResponse = mock(OrderResponse.class);
        when(orderService.getOrderById(50L)).thenReturn(orderResponse);

        mockMvc.perform(get("/employee-panel/orders/50")
                        .with(user("employee@test.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-panel/order/employee-order-details-page"))
                .andExpect(model().attributeExists("order", "activePage"))
                .andExpect(model().attribute("order", orderResponse))
                .andExpect(model().attribute("activePage", "orders"));

        verify(orderService, times(1)).getOrderById(50L);
    }

    @Test
    @DisplayName("POST /employee-panel/orders/{id}/approve - Should redirect when unauthorized")
    void processApproveOrder_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/employee-panel/orders/50/approve")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).approveOrder(anyLong());
    }

    @Test
    @DisplayName("POST /employee-panel/orders/{id}/approve - Should approve order and redirect to /employee-panel/orders")
    void processApproveOrder_WithEmployeeUser_ShouldApproveAndRedirect() throws Exception {
        mockMvc.perform(post("/employee-panel/orders/50/approve")
                        .with(user("employee@test.com").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-panel/orders"));

        verify(orderService, times(1)).approveOrder(50L);
    }
}