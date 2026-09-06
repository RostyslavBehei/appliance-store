package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.VerificationToken;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import com.epam.rd.autocode.assessment.appliances.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceImplTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private VerificationTokenServiceImpl verificationTokenService;

    private User testUser;
    private VerificationToken validToken;
    private VerificationToken expiredToken;

    @BeforeEach
    void setUp() {
        testUser = mock(User.class);

        validToken = VerificationToken.builder()
                .id(1L)
                .token("valid-uuid-token")
                .user(testUser)
                .tokenAction(TokenAction.VERIFY_ACCOUNT)
                .expiryDate(LocalDateTime.now().plusMinutes(60))
                .build();

        expiredToken = VerificationToken.builder()
                .id(2L)
                .token("expired-uuid-token")
                .user(testUser)
                .tokenAction(TokenAction.RESET_PASSWORD)
                .expiryDate(LocalDateTime.now().minusMinutes(10))
                .build();

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Generate Token - Should delete old token, generate and save new one")
    void generateToken_ShouldDeleteOldAndSaveNew() {
        when(verificationTokenRepository.existsByToken(anyString())).thenReturn(false);

        String resultToken = verificationTokenService.generateToken(testUser, TokenAction.VERIFY_ACCOUNT, 60);

        assertNotNull(resultToken);
        assertFalse(resultToken.isBlank());

        verify(verificationTokenRepository, times(1)).deleteByUserAndTokenAction(testUser, TokenAction.VERIFY_ACCOUNT);
        verify(verificationTokenRepository, atLeastOnce()).existsByToken(anyString());
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Validate Token - Should return User and delete token if valid")
    void validateToken_ShouldReturnUserAndDeleteToken_IfValid() {
        String token = "valid-uuid-token";
        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(validToken));

        Optional<User> result = verificationTokenService.validateToken(token);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());

        verify(verificationTokenRepository, times(1)).delete(validToken);
    }

    @Test
    @DisplayName("Validate Token - Should return empty Optional if token is expired")
    void validateToken_ShouldReturnEmptyOptional_IfExpired() {
        String token = "expired-uuid-token";
        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));

        Optional<User> result = verificationTokenService.validateToken(token);

        assertFalse(result.isPresent());

        verify(verificationTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Validate Token - Should throw NotFoundException if token not exist")
    void validateToken_ShouldThrowNotFoundException_IfNotExist() {
        String token = "unknown-token";
        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> verificationTokenService.validateToken(token));

        assertEquals("error.token.not.found", exception.getMessage());
        verify(verificationTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Is Token Valid - Should return true if token is not expired")
    void isTokenValid_ShouldReturnTrue_IfNotExpired() {
        String token = "valid-uuid-token";
        TokenAction action = TokenAction.VERIFY_ACCOUNT;
        when(verificationTokenRepository.findByTokenAndTokenAction(token, action)).thenReturn(Optional.of(validToken));

        boolean isValid = verificationTokenService.isTokenValid(token, action);

        assertTrue(isValid);
        verify(verificationTokenRepository, times(1)).findByTokenAndTokenAction(token, action);
    }

    @Test
    @DisplayName("Is Token Valid - Should return false if token is expired")
    void isTokenValid_ShouldReturnFalse_IfExpired() {
        String token = "expired-uuid-token";
        TokenAction action = TokenAction.RESET_PASSWORD;
        when(verificationTokenRepository.findByTokenAndTokenAction(token, action)).thenReturn(Optional.of(expiredToken));

        boolean isValid = verificationTokenService.isTokenValid(token, action);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Is Token Valid - Should throw NotFoundException if token not found by action")
    void isTokenValid_ShouldThrowNotFoundException_IfNotFound() {
        String token = "unknown-token";
        TokenAction action = TokenAction.VERIFY_ACCOUNT;
        when(verificationTokenRepository.findByTokenAndTokenAction(token, action)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> verificationTokenService.isTokenValid(token, action));

        assertEquals("error.token.not.found", exception.getMessage());
    }
}