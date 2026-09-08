package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserEmailChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

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

    private final String clientEmail = "client@test.com";

    @Test
    @DisplayName("GET /profile - Should redirect to login when unauthorized")
    void showProfilePage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    @DisplayName("GET /profile - Should return profile page with isClient=true for ClientProfileResponse")
    void showProfilePage_ClientUser_ShouldReturnProfileView() throws Exception {
        ClientProfileResponse clientProfile = mock(ClientProfileResponse.class);
        when(userService.getUserByEmail(clientEmail)).thenReturn(clientProfile);

        mockMvc.perform(get("/profile")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/user-profile-page"))
                .andExpect(model().attributeExists("user", "isClient", "isEmployee"))
                .andExpect(model().attribute("user", clientProfile))
                .andExpect(model().attribute("isClient", true))
                .andExpect(model().attribute("isEmployee", false));

        verify(userService, times(1)).getUserByEmail(clientEmail);
    }

    @Test
    @DisplayName("GET /profile - Should return profile page with isEmployee=true for EmployeeProfileResponse")
    void showProfilePage_EmployeeUser_ShouldReturnProfileView() throws Exception {
        EmployeeProfileResponse employeeProfile = mock(EmployeeProfileResponse.class);
        when(userService.getUserByEmail(clientEmail)).thenReturn(employeeProfile);

        mockMvc.perform(get("/profile")
                        .with(user(clientEmail).roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/user-profile-page"))
                .andExpect(model().attribute("user", employeeProfile))
                .andExpect(model().attribute("isClient", false))
                .andExpect(model().attribute("isEmployee", true));

        verify(userService, times(1)).getUserByEmail(clientEmail);
    }

    @Test
    @DisplayName("GET /profile/update - Should redirect to login when unauthorized")
    void showUpdateProfilePage_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/profile/update"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    @DisplayName("GET /profile/update - Should return update profile view")
    void showUpdateProfilePage_WithPrincipal_ShouldReturnUpdateView() throws Exception {
        ClientProfileResponse clientProfile = mock(ClientProfileResponse.class);
        when(userService.getUserByEmail(clientEmail)).thenReturn(clientProfile);

        mockMvc.perform(get("/profile/update")
                        .with(user(clientEmail).roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/user-profile-update-page"))
                .andExpect(model().attributeExists("user", "isClient", "isEmployee"))
                .andExpect(model().attribute("isClient", true))
                .andExpect(model().attribute("isEmployee", false));

        verify(userService, times(1)).getUserByEmail(clientEmail);
    }

    @Test
    @DisplayName("POST /profile/change-password - Should redirect to login when unauthorized")
    void processChangePassword_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .param("oldPassword", "oldPass123!")
                        .param("newPassword", "newPass123!")
                        .param("confirmPassword", "newPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(userService, never()).updatePassword(anyString(), any());
    }

    @Test
    @DisplayName("POST /profile/change-password - Should change password, logout and redirect to login")
    void processChangePassword_ValidRequest_ShouldChangePasswordAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("oldPassword", "oldPass123!")
                        .param("newPassword", "newPass123!")
                        .param("confirmPassword", "newPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged=true"));

        verify(userService, times(1)).updatePassword(eq(clientEmail), any(UserPasswordChangeRequest.class));
    }

    @Test
    @DisplayName("POST /profile/change-password - Should redirect to update with error on validation failure")
    void processChangePassword_BindingErrors_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("oldPassword", "")
                        .param("newPassword", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/update?error=passwordValidation"));

        verify(userService, never()).updatePassword(anyString(), any());
    }

    @Test
    @DisplayName("POST /profile/change-password - Should redirect with error when IllegalArgumentException occurs")
    void processChangePassword_IllegalArgumentException_ShouldRedirectWithError() throws Exception {
        doThrow(new IllegalArgumentException("Old password does not match"))
                .when(userService).updatePassword(eq(clientEmail), any(UserPasswordChangeRequest.class));

        mockMvc.perform(post("/profile/change-password")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("oldPassword", "wrongPass123!")
                        .param("newPassword", "newPass123!")
                        .param("confirmPassword", "newPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/update?error=password"));

        verify(userService, times(1)).updatePassword(eq(clientEmail), any(UserPasswordChangeRequest.class));
    }

    @Test
    @DisplayName("POST /profile/change-email - Should redirect to login when unauthorized")
    void processChangeEmail_WithoutPrincipal_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/profile/change-email")
                        .with(csrf())
                        .param("newEmail", "new@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/google"));

        verify(userService, never()).updateEmail(anyString(), any());
    }

    @Test
    @DisplayName("POST /profile/change-email - Should change email, logout and redirect to login")
    void processChangeEmail_ValidRequest_ShouldChangeEmailAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/change-email")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("newEmail", "updated@test.com")
                        .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailChanged=true"));

        verify(userService, times(1)).updateEmail(eq(clientEmail), any(UserEmailChangeRequest.class));
    }

    @Test
    @DisplayName("POST /profile/change-email - Should redirect to update with error on validation failure")
    void processChangeEmail_BindingErrors_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/profile/change-email")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("newEmail", "invalid-email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/update?error=emailValidation"));

        verify(userService, never()).updateEmail(anyString(), any());
    }

    @Test
    @DisplayName("POST /profile/change-email - Should redirect with error on IllegalArgumentException")
    void processChangeEmail_IllegalArgumentException_ShouldRedirectWithError() throws Exception {
        doThrow(new IllegalArgumentException("Email already taken"))
                .when(userService).updateEmail(eq(clientEmail), any(UserEmailChangeRequest.class));

        mockMvc.perform(post("/profile/change-email")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("newEmail", "taken@test.com")
                        .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/update?error=email"));

        verify(userService, times(1)).updateEmail(eq(clientEmail), any(UserEmailChangeRequest.class));
    }

    @Test
    @DisplayName("POST /profile/update - Should update profile and redirect to /profile")
    void processUpdateProfile_ValidRequest_ShouldUpdateAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("middleName", "Middle")
                        .param("phone", "+380980000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(userService, times(1)).updateUser(eq(clientEmail), any(UserProfileUpdateRequest.class));
    }

    @Test
    @DisplayName("POST /profile/deactivate - Should delete user, logout and redirect to login?logout")
    void processDeactivateProfile_ShouldDeleteAccountAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/deactivate")
                        .with(user(clientEmail).roles("CLIENT"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        verify(userService, times(1)).deleteUserByEmail(clientEmail);
    }
}