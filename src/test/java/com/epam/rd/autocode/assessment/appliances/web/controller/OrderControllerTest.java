package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderCheckoutRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    @MockBean
    private ApplianceService applianceService;

    @MockBean
    private ManufacturerService manufacturerService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private JwtCore jwtCore;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final String clientEmail = "client@test.com";
    private CartResponse testCartResponse;
    private OrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        ApplianceSummaryResponse appliance = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-100",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(150.0)
        );

        CartItemResponse cartItem = new CartItemResponse(
                10L,
                appliance,
                2,
                BigDecimal.valueOf(300.0)
        );

        testCartResponse = new CartResponse(1L, List.of(cartItem));
        testOrderResponse = mock(OrderResponse.class);
    }

    @Test
    @DisplayName("GET /orders - Should redirect to login when user is unauthorized")
    void showOrdersPage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).getClientOrders(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /orders - Should return orders page with pageable (0, 8)")
    void showOrdersPage_WithPrincipal_ShouldReturnViewAndOrders() throws Exception {
        Page<OrderResponse> ordersPage = new PageImpl<>(List.of(testOrderResponse));
        Pageable expectedPageable = PageRequest.of(0, 8);

        when(orderService.getClientOrders(clientEmail, expectedPageable)).thenReturn(ordersPage);

        mockMvc.perform(get("/orders")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("order/orders-page"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", ordersPage));

        verify(orderService, times(1)).getClientOrders(clientEmail, expectedPageable);
    }

    @Test
    @DisplayName("GET /orders/checkout - Should redirect to login when user is unauthorized")
    void showOrderCheckoutPage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(cartService, never()).getAllCarts(anyString());
    }

    @Test
    @DisplayName("GET /orders/checkout - Should return checkout page with populated cart summary")
    void showOrderCheckoutPage_WithPrincipal_ShouldReturnViewAndModel() throws Exception {
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(testCartResponse));
        when(cartService.getCartItemCount(clientEmail)).thenReturn(2);

        mockMvc.perform(get("/orders/checkout")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-checkout-page"))
                .andExpect(model().attributeExists("carts", "totalItems", "totalSum", "checkoutRequest"))
                .andExpect(model().attribute("totalItems", 2))
                .andExpect(model().attribute("totalSum", BigDecimal.valueOf(300.0)));

        verify(cartService, times(1)).getAllCarts(clientEmail);
        verify(cartService, times(1)).getCartItemCount(clientEmail);
    }

    @Test
    @DisplayName("GET /orders/{id} - Should redirect to login when user is unauthorized")
    void showOrderDetailsPage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).getOrderById(anyLong());
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return order details page")
    void showOrderDetailsPage_WithPrincipal_ShouldReturnViewAndOrder() throws Exception {
        when(orderService.getOrderById(100L)).thenReturn(testOrderResponse);

        mockMvc.perform(get("/orders/100")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-details-page"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", testOrderResponse));

        verify(orderService, times(1)).getOrderById(100L);
    }

    @Test
    @DisplayName("POST /orders - Should redirect to login when user is unauthorized")
    void processCartCheckout_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(orderService, never()).checkout(anyString(), any(OrderCheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /orders - Should successfully place order and redirect to /orders")
    void processCartCheckout_ValidRequest_ShouldPlaceOrderAndRedirect() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", clientEmail)
                        .param("phone", "+380980000000")
                        .param("country", "Ukraine")
                        .param("city", "Lviv")
                        .param("street", "Volodymyra Velykoho")
                        .param("zipCode", "79000")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService, times(1)).checkout(eq(clientEmail), any(OrderCheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /orders - Should return checkout view with binding errors on invalid input")
    void processCartCheckout_ValidationErrors_ShouldReturnCheckoutForm() throws Exception {
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(testCartResponse));
        when(cartService.getCartItemCount(clientEmail)).thenReturn(2);

        mockMvc.perform(post("/orders")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("firstName", "")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-checkout-page"))
                .andExpect(model().attributeExists("carts", "totalItems", "totalSum"))
                .andExpect(model().hasErrors());

        verify(orderService, never()).checkout(anyString(), any(OrderCheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /orders - Should return checkout page with error message on IllegalStateException (e.g. empty cart)")
    void processCartCheckout_IllegalStateException_ShouldReturnCheckoutViewWithError() throws Exception {
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(testCartResponse));
        when(cartService.getCartItemCount(clientEmail)).thenReturn(2);

        doThrow(new IllegalStateException("Cart is empty"))
                .when(orderService).checkout(eq(clientEmail), any(OrderCheckoutRequest.class));

        mockMvc.perform(post("/orders")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", clientEmail)
                        .param("phone", "+380980000000")
                        .param("country", "Ukraine")
                        .param("city", "Lviv")
                        .param("street", "Volodymyra Velykoho")
                        .param("zipCode", "79000")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-checkout-page"))
                .andExpect(model().attributeExists("errorMessage", "carts", "totalItems", "totalSum"))
                .andExpect(model().attribute("errorMessage", "Cart is empty"));

        verify(orderService, times(1)).checkout(eq(clientEmail), any(OrderCheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /orders - Should return checkout page with general error on unexpected exception")
    void processCartCheckout_UnexpectedException_ShouldReturnCheckoutViewWithGenericError() throws Exception {
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(testCartResponse));
        when(cartService.getCartItemCount(clientEmail)).thenReturn(2);

        doThrow(new RuntimeException("Database error"))
                .when(orderService).checkout(eq(clientEmail), any(OrderCheckoutRequest.class));

        mockMvc.perform(post("/orders")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", clientEmail)
                        .param("phone", "+380980000000")
                        .param("country", "Ukraine")
                        .param("city", "Lviv")
                        .param("street", "Volodymyra Velykoho")
                        .param("zipCode", "79000")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-checkout-page"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "An error occurred while placing your order. Please try again."));

        verify(orderService, times(1)).checkout(eq(clientEmail), any(OrderCheckoutRequest.class));
    }
}