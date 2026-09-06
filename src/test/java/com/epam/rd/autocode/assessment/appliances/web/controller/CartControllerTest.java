package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemResponse;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

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

    private CartResponse testCartResponse;
    private final String clientEmail = "client@test.com";

    @BeforeEach
    void setUp() {
        ApplianceSummaryResponse appliance = new ApplianceSummaryResponse(
                1L,
                "Microwave",
                "MW-200",
                "BIG",
                "Samsung",
                BigDecimal.valueOf(100.0)
        );

        CartItemResponse item1 = new CartItemResponse(
                10L,
                appliance,
                2,
                BigDecimal.valueOf(200.0)
        );

        CartItemResponse item2 = new CartItemResponse(
                11L,
                appliance,
                1,
                BigDecimal.valueOf(100.0)
        );

        testCartResponse = new CartResponse(1L, List.of(item1, item2));
    }

    @Test
    @DisplayName("GET /cart - Should redirect when unauthorized")
    void showCartPage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(cartService, never()).getAllCarts(anyString());
    }

    @Test
    @DisplayName("GET /cart - Should return view with carts, totalSum and totalItems when authenticated")
    void showCartPage_WithPrincipal_ShouldReturnViewAndModel() throws Exception {
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(testCartResponse));

        mockMvc.perform(get("/cart")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart-page"))
                .andExpect(model().attributeExists("carts", "totalSum", "totalItems"))
                .andExpect(model().attribute("totalSum", BigDecimal.valueOf(300.0)))
                .andExpect(model().attribute("totalItems", 2L));

        verify(cartService, times(1)).getAllCarts(clientEmail);
    }

    @Test
    @DisplayName("GET /cart - Should handle empty cart gracefully with zero sums")
    void showCartPage_WhenCartEmpty_ShouldReturnZeroTotals() throws Exception {
        CartResponse emptyCart = new CartResponse(1L, Collections.emptyList());
        when(cartService.getAllCarts(clientEmail)).thenReturn(List.of(emptyCart));

        mockMvc.perform(get("/cart")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart-page"))
                .andExpect(model().attribute("totalSum", BigDecimal.ZERO))
                .andExpect(model().attribute("totalItems", 0L));

        verify(cartService, times(1)).getAllCarts(clientEmail);
    }

    @Test
    @DisplayName("POST /cart/add - Should redirect when unauthorized")
    void addCartItem_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("clientEmail", clientEmail)
                        .param("applianceId", "1")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(cartService, never()).addToCart(any());
    }

    @Test
    @DisplayName("POST /cart/add - Should add item and redirect to default /cart when Referer header is missing")
    void addCartItem_WithoutReferer_ShouldAddToCartAndRedirectToCart() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("clientEmail", clientEmail)
                        .param("applianceId", "1")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).addToCart(argThat(request ->
                request.clientEmail().equals(clientEmail) &&
                        request.applianceId().equals(1L) &&
                        request.quantity() == 2
        ));
    }

    @Test
    @DisplayName("POST /cart/add - Should add item and redirect back to relative safe Referer")
    void addCartItem_WithRelativeReferer_ShouldRedirectToReferer() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .header("Referer", "/category/BIG")
                        .param("clientEmail", clientEmail)
                        .param("applianceId", "1")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/category/BIG"));

        verify(cartService, times(1)).addToCart(any(CartItemAddRequest.class));
    }

    @Test
    @DisplayName("POST /cart/add - Should add item and redirect back to safe absolute same-host Referer")
    void addCartItem_WithSameHostReferer_ShouldRedirectToReferer() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .header("Referer", "http://localhost/appliances/1")
                        .param("clientEmail", clientEmail)
                        .param("applianceId", "1")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/appliances/1"));

        verify(cartService, times(1)).addToCart(any(CartItemAddRequest.class));
    }

    @Test
    @DisplayName("POST /cart/add - Should fallback to /cart if Referer is external untrusted domain")
    void addCartItem_WithUnsafeReferer_ShouldFallbackToCart() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .header("Referer", "https://evil-phishing-site.com/hack")
                        .param("clientEmail", clientEmail)
                        .param("applianceId", "1")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).addToCart(any(CartItemAddRequest.class));
    }

    @Test
    @DisplayName("POST /cart/add - Should not call service if binding/validation error occurs")
    void addCartItem_WithBindingError_ShouldRedirectWithoutCallingService() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("applianceId", "not-a-number")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, never()).addToCart(any());
    }

    @Test
    @DisplayName("POST /cart/update - Should update quantity and redirect to referer")
    void updateCartItemQuantity_ShouldUpdateAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/update")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("cartItemId", "10")
                        .param("quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).updateCartItemQuantity(10L, 5);
    }

    @Test
    @DisplayName("POST /cart/update - Should redirect to /cart?error=true when exception occurs")
    void updateCartItemQuantity_OnException_ShouldRedirectToError() throws Exception {
        doThrow(new RuntimeException("Cart item not found"))
                .when(cartService).updateCartItemQuantity(99L, 3);

        mockMvc.perform(post("/cart/update")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("cartItemId", "99")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?error=true"));

        verify(cartService, times(1)).updateCartItemQuantity(99L, 3);
    }

    @Test
    @DisplayName("POST /cart/remove - Should delete item and redirect to referer")
    void removeCartItem_ShouldDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/remove")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .header("Referer", "/cart")
                        .param("cartItemId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).deleteCartItem(10L);
    }
}