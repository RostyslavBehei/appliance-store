package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

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

@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerTest {

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
    @DisplayName("GET /admin/orders - Should redirect to login when unauthorized")
    void showOrdersPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).getAllOrdersSummary(any(), any());
    }

    @Test
    @DisplayName("GET /admin/orders - Should return orders list with default pagination and newest sort")
    void showOrdersPage_DefaultParams_ShouldReturnOrdersView() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(
                List.of(summary),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(orderService.getAllOrdersSummary(isNull(), any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/admin/orders")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/admin-orders-page"))
                .andExpect(model().attributeExists("ordersPage", "activePage", "sort"))
                .andExpect(model().attribute("ordersPage", pageResult))
                .andExpect(model().attribute("sort", "newest"))
                .andExpect(model().attribute("activePage", "orders"))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(orderService, times(1)).getAllOrdersSummary(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /admin/orders - Should apply oldest sort, keyword, and custom pagination")
    void showOrdersPage_CustomParams_ShouldPassToService() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(List.of(summary));

        when(orderService.getAllOrdersSummary(eq("client@test.com"), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/admin/orders")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "oldest")
                        .param("keyword", "client@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/admin-orders-page"))
                .andExpect(model().attribute("ordersPage", pageResult))
                .andExpect(model().attribute("keyword", "client@test.com"))
                .andExpect(model().attribute("sort", "oldest"))
                .andExpect(model().attribute("activePage", "orders"));

        verify(orderService, times(1)).getAllOrdersSummary(
                eq("client@test.com"),
                eq(PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /admin/orders - Should normalize negative page parameter to 0")
    void showOrdersPage_NegativePage_ShouldNormalizeToZero() throws Exception {
        OrderSummaryResponse summary = mock(OrderSummaryResponse.class);
        Page<OrderSummaryResponse> pageResult = new PageImpl<>(List.of(summary));

        when(orderService.getAllOrdersSummary(isNull(), any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/admin/orders")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/admin-orders-page"));

        verify(orderService, times(1)).getAllOrdersSummary(
                isNull(),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @Test
    @DisplayName("GET /admin/orders/{id} - Should return order details page")
    void showOrdersDetailPage_ShouldReturnDetailsView() throws Exception {
        OrderResponse orderResponse = mock(OrderResponse.class);
        when(orderService.getOrderById(100L)).thenReturn(orderResponse);

        mockMvc.perform(get("/admin/orders/100")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/admin-orders-details-page"))
                .andExpect(model().attributeExists("order", "activePage"))
                .andExpect(model().attribute("order", orderResponse))
                .andExpect(model().attribute("activePage", "orders"));

        verify(orderService, times(1)).getOrderById(100L);
    }

    @Test
    @DisplayName("POST /admin/orders/{id}/approve - Should approve order and redirect to details page")
    void processOrderApprove_ShouldApproveAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/orders/100/approve")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/100"));

        verify(orderService, times(1)).approveOrder(100L);
    }

    @Test
    @DisplayName("POST /admin/orders/{id}/approve - Should redirect when unauthorized")
    void processOrderApprove_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/orders/100/approve")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).approveOrder(anyLong());
    }
}