package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ClientService clientService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtCore jwtCore;

    @MockBean
    private CartService cartService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private ManufacturerService manufacturerService;

    @Test
    @DisplayName("GET /login - Should return login form")
    void showLoginForm_ShouldReturnViewAndModel() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-login-basic"))
                .andExpect(model().attributeExists("loginRequest"));
    }

    @Test
    @DisplayName("POST /login - Should authenticate and set JWT cookie")
    void processLogin_ShouldAuthenticateAndRedirect() throws Exception {
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtCore.generateToken(userDetails)).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "test@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().value("jwt", "mocked-jwt-token"));

        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtCore, times(1)).generateToken(userDetails);
    }

    @Test
    @DisplayName("POST /login - Should redirect to login?error=true on bad credentials")
    void processLogin_ShouldRedirectOnError() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "wrong@test.com")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("POST /login - Should return view on validation errors")
    void processLogin_ShouldReturnViewOnValidationError() throws Exception {
        mockMvc.perform(post("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-login-basic"))
                .andExpect(model().hasErrors());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("GET /register - Should return register form")
    void showRegisterForm_ShouldReturnViewAndModel() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-register-basic"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    @DisplayName("POST /register - Should successfully register and redirect to login")
    void processRegister_ShouldRegisterAndRedirect() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "new@test.com")
                        .param("password", "Pass1234!")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clientService, times(1)).createClient(any(ClientRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /register - Should return form view with errorMessage when exception occurs")
    void processRegister_ShouldRedirectOnException() throws Exception {
        doThrow(new RuntimeException("DB Error")).when(clientService).createClient(any());

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "new@test.com")
                        .param("password", "Pass1234!")
                        .param("firstName", "John"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-register-basic"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Registration failed: DB Error"));

        verify(clientService, times(1)).createClient(any(ClientRegisterRequest.class));
    }

    @Test
    @DisplayName("GET /forgot-password - Should return forgot password page")
    void showForgotPasswordPage_ShouldReturnView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-forgot-password-page"));
    }

    @Test
    @DisplayName("POST /forgot-password - Should process request and redirect to login")
    void processForgotPassword_ShouldRedirectToLogin() throws Exception {
        String email = "test@test.com";

        mockMvc.perform(post("/forgot-password").with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).forgotPassword(email);
    }

    @Test
    @DisplayName("GET /reset-password - Should return reset password page if token is present")
    void showResetPasswordPage_ShouldReturnViewIfTokenPresent() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/auth-reset-password-page"))
                .andExpect(model().attributeExists("token"))
                .andExpect(model().attribute("token", "valid-token"));
    }

    @Test
    @DisplayName("GET /reset-password - Should redirect to login if token is missing or empty")
    void showResetPasswordPage_ShouldRedirectToLoginIfTokenMissing() throws Exception {
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/reset-password").param("token", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /reset-password - Should process request and redirect to login")
    void processResetPassword_ShouldProcessAndRedirect() throws Exception {
        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", "valid-token")
                        .param("newPassword", "newPass")
                        .param("confirmPassword", "newPass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).resetPassword(eq("valid-token"), any(UserPasswordResetRequest.class));
    }

    @Test
    @DisplayName("GET /verify - Should verify token and redirect to login")
    void verify_ShouldVerifyTokenAndRedirectToLogin() throws Exception {
        mockMvc.perform(get("/verify").param("token", "verification-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified=true"));

        verify(userService, times(1)).verifyAccount("verification-token");
    }
}