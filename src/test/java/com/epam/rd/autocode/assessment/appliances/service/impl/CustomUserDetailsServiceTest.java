package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .build();

        lenient().when(messageSource.getMessage(any(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Load User By Username - Should successfully return result")
    void loadUserByUsername_ShouldReturnResult() {
        String username = "test@test.com";

        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        assertEquals(username, result.getUsername());

        verify(userRepository).findByEmail(username);
    }

    @Test
    @DisplayName("Load User By Username - Should throw UsernameNotFoundException if user not exist")
    void loadUserByUsername_ShouldThrowNotFoundException() {
        String username = "fantom@test.com";

        when(userRepository.findByEmail(username)).thenReturn(Optional.empty());

        UsernameNotFoundException result = assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername(username));

        assertEquals("error.user.not.found", result.getMessage());

        verify(userRepository).findByEmail(username);
    }
}