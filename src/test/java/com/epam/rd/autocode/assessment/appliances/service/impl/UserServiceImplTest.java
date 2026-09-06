package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.*;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import com.epam.rd.autocode.assessment.appliances.service.VerificationTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private MailService mailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationTokenService verificationTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private UserServiceImpl userService;

    private Client testClient;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
                .id(1L)
                .email("client@test.com")
                .password("encoded_pass")
                .firstName("John")
                .lastName("Doe")
                .cart("1234-5678")
                .role(Role.ROLE_CLIENT)
                .enabled(true)
                .build();

        testEmployee = new Employee();
        testEmployee.setId(2L);
        testEmployee.setEmail("employee@test.com");
        testEmployee.setFirstName("Jane");
        testEmployee.setLastName("Smith");
        testEmployee.setRole(Role.ROLE_EMPLOYEE);
        testEmployee.setDepartment("Sales");
        testEmployee.setEnabled(true);

        ReflectionTestUtils.setField(userService, "BASE_URL", "http://localhost:8080");

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String role) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        lenient().doReturn(List.of(new SimpleGrantedAuthority(role)))
                .when(authentication).getAuthorities();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Get User By Email - Should return Client Profile")
    void getUserByEmail_ShouldReturnClientProfile() {
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));

        UserProfileResponse result = userService.getUserByEmail("client@test.com");

        assertNotNull(result);
        assertInstanceOf(ClientProfileResponse.class, result);
        verify(userRepository, times(1)).findByEmail("client@test.com");
    }

    @Test
    @DisplayName("Get User By Email - Should return Employee Profile")
    void getUserByEmail_ShouldReturnEmployeeProfile() {
        when(userRepository.findByEmail("employee@test.com")).thenReturn(Optional.of(testEmployee));

        UserProfileResponse result = userService.getUserByEmail("employee@test.com");

        assertNotNull(result);
        assertInstanceOf(EmployeeProfileResponse.class, result);
        verify(userRepository, times(1)).findByEmail("employee@test.com");
    }

    @Test
    @DisplayName("Get User By Email - Should throw NotFoundException if user does not exist")
    void getUserByEmail_ShouldThrowNotFoundException_WhenUserNotExists() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.getUserByEmail("unknown@test.com"));

        assertEquals("error.user.not.found", result.getMessage());
    }

    @Test
    @DisplayName("Get User By Email - Should throw IllegalStateException for unknown user subtype")
    void getUserByEmail_ShouldThrowExceptionForUnknownType() {
        User unknownUser = new User() {};
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.of(unknownUser));

        IllegalStateException result = assertThrows(IllegalStateException.class,
                () -> userService.getUserByEmail("unknown@test.com"));

        assertEquals("error.user.unknown.type", result.getMessage());
    }

    @Test
    @DisplayName("Update User - Should successfully update basic fields for standard user")
    void updateUser_ShouldUpdateBasicFields_WhenNotAdmin() {
        mockSecurityContext("ROLE_CLIENT");

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "UpdatedName", "UpdatedLast", "UpdatedMiddle",
                LocalDate.of(1995, 5, 20), null, null, null, null
        );

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));

        userService.updateUser("client@test.com", request);

        assertEquals("UpdatedName", testClient.getFirstName());
        assertEquals("UpdatedLast", testClient.getLastName());
        assertEquals("UpdatedMiddle", testClient.getMiddleName());
        assertEquals(LocalDate.of(1995, 5, 20), testClient.getBirthday());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Update User - Should update role, cart and enabled fields for Admin")
    void updateUser_ShouldUpdateSecureFields_WhenAdmin() {
        mockSecurityContext("ROLE_ADMIN");

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, null, null,
                Role.ROLE_EMPLOYEE, "9999-9999", null, false
        );

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));

        userService.updateUser("client@test.com", request);

        assertEquals(Role.ROLE_EMPLOYEE, testClient.getRole());
        assertEquals("9999-9999", testClient.getCart());
        assertFalse(testClient.getEnabled());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Update User - Should update department for Employee when Admin")
    void updateUser_ShouldUpdateDepartment_WhenEmployeeAndAdmin() {
        mockSecurityContext("ROLE_ADMIN");

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, null, null, null, null, "Marketing", true
        );

        when(userRepository.findByEmail("employee@test.com")).thenReturn(Optional.of(testEmployee));

        userService.updateUser("employee@test.com", request);

        assertEquals("Marketing", testEmployee.getDepartment());
        verify(userRepository, times(1)).save(testEmployee);
    }

    @Test
    @DisplayName("Forgot Password - Should generate token and send email")
    void forgotPassword_ShouldGenerateTokenAndSendEmail() {
        String email = "client@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testClient));
        when(verificationTokenService.generateToken(testClient, TokenAction.RESET_PASSWORD, 60))
                .thenReturn("mockedToken");

        userService.forgotPassword(email);

        verify(verificationTokenService, times(1)).generateToken(testClient, TokenAction.RESET_PASSWORD, 60);
        verify(mailService, times(1)).sendPasswordResetMessage(
                email, "http://localhost:8080/reset-password?token=mockedToken");
    }

    @Test
    @DisplayName("Forgot Password - Should throw NotFoundException if email does not exist")
    void forgotPassword_ShouldThrowNotFoundException_WhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.forgotPassword("unknown@test.com"));

        assertEquals("error.user.not.found", result.getMessage());
        verify(verificationTokenService, never()).generateToken(any(), any(), anyInt());
        verify(mailService, never()).sendPasswordResetMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Update Password - Should successfully update password")
    void updatePassword_ShouldSuccessfullyUpdate() {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest("oldPass", "newPass", "newPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("oldPass", testClient.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPass", testClient.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        userService.updatePassword("client@test.com", request);

        assertEquals("encodedNewPass", testClient.getPassword());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Update Password - Should throw exception if new passwords don't match")
    void updatePassword_ShouldThrowException_IfPasswordsMismatch() {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest("oldPass", "newPass", "differentPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.updatePassword("client@test.com", request));

        assertEquals("error.password.mismatch", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Password - Should throw exception if old password is incorrect")
    void updatePassword_ShouldThrowException_IfOldPasswordIncorrect() {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest("wrongOldPass", "newPass", "newPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrongOldPass", testClient.getPassword())).thenReturn(false);

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.updatePassword("client@test.com", request));

        assertEquals("error.password.old.mismatch", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Password - Should throw exception if new password matches old password")
    void updatePassword_ShouldThrowException_IfNewPasswordSameAsOld() {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest("oldPass", "oldPass", "oldPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("oldPass", testClient.getPassword())).thenReturn(true);

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.updatePassword("client@test.com", request));

        assertEquals("error.password.new.same", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset Password - Should successfully reset password with valid token")
    void resetPassword_ShouldSuccessfullyReset() {
        UserPasswordResetRequest request = new UserPasswordResetRequest("newPass", "newPass");

        when(verificationTokenService.validateToken("validToken")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        userService.resetPassword("validToken", request);

        assertEquals("encodedNewPass", testClient.getPassword());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Reset Password - Should throw exception if passwords mismatch")
    void resetPassword_ShouldThrowException_WhenPasswordsMismatch() {
        UserPasswordResetRequest request = new UserPasswordResetRequest("newPass", "wrongConfirm");

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.resetPassword("token", request));

        assertEquals("error.password.mismatch", result.getMessage());
        verify(verificationTokenService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Reset Password - Should throw NotFoundException if token is invalid or expired")
    void resetPassword_ShouldThrowNotFoundException_WhenTokenInvalid() {
        UserPasswordResetRequest request = new UserPasswordResetRequest("newPass", "newPass");

        when(verificationTokenService.validateToken("expiredToken")).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.resetPassword("expiredToken", request));

        assertEquals("error.token.not.found", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Email - Should successfully update email")
    void updateEmail_ShouldSuccessfullyUpdate() {
        UserEmailChangeRequest request = new UserEmailChangeRequest("new@test.com", "correctPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("correctPass", testClient.getPassword())).thenReturn(true);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        userService.updateEmail("client@test.com", request);

        assertEquals("new@test.com", testClient.getEmail());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Update Email - Should throw exception if password is incorrect")
    void updateEmail_ShouldThrowException_WhenPasswordIncorrect() {
        UserEmailChangeRequest request = new UserEmailChangeRequest("new@test.com", "wrongPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrongPass", testClient.getPassword())).thenReturn(false);

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.updateEmail("client@test.com", request));

        assertEquals("error.password.incorrect", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Email - Should throw exception if new email is already taken")
    void updateEmail_ShouldThrowException_IfEmailTaken() {
        UserEmailChangeRequest request = new UserEmailChangeRequest("taken@test.com", "correctPass");

        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("correctPass", testClient.getPassword())).thenReturn(true);
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User() {}));

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> userService.updateEmail("client@test.com", request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify Account - Should activate user account with valid token")
    void verifyAccount_ShouldActivateAccount() {
        testClient.setEnabled(false);
        when(verificationTokenService.validateToken("validToken")).thenReturn(Optional.of(testClient));

        userService.verifyAccount("validToken");

        assertTrue(testClient.getEnabled());
        verify(userRepository, times(1)).save(testClient);
    }

    @Test
    @DisplayName("Verify Account - Should throw NotFoundException if token is invalid")
    void verifyAccount_ShouldThrowNotFoundException_WhenTokenInvalid() {
        when(verificationTokenService.validateToken("invalidToken")).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.verifyAccount("invalidToken"));

        assertEquals("error.user.token.not.valid", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete User By Id - Should delete entity via repository.delete")
    void deleteUserById_ShouldDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));

        userService.deleteUserById(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(testClient);
    }

    @Test
    @DisplayName("Delete User By Id - Should throw NotFoundException if user does not exist")
    void deleteUserById_ShouldThrowNotFoundException_WhenUserNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.deleteUserById(99L));

        assertEquals("error.user.id.not.found", result.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete User By Email - Should delete entity via repository.delete")
    void deleteUserByEmail_ShouldDelete() {
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(testClient));

        userService.deleteUserByEmail("client@test.com");

        verify(userRepository, times(1)).findByEmail("client@test.com");
        verify(userRepository, times(1)).delete(testClient);
    }

    @Test
    @DisplayName("Delete User By Email - Should throw NotFoundException if user does not exist")
    void deleteUserByEmail_ShouldThrowNotFoundException_WhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> userService.deleteUserByEmail("unknown@test.com"));

        assertEquals("error.user.not.found", result.getMessage());
        verify(userRepository, never()).delete(any());
    }
}