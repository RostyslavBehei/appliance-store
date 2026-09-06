package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.*;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.VerificationTokenRepository;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import com.epam.rd.autocode.assessment.appliances.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${app.base-url}")
    private String BASE_URL;

    private final MailService mailService;
    private final VerificationTokenService verificationTokenService;

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByEmail(String userEmail) {
        log.debug("Fetching profile for user: '{}'", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("Failed to fetch profile: User '{}' not found", userEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{userEmail}, getLocale())
                    );
                });

        if (user instanceof Client client) {
            return ClientProfileResponse.fromEntity(client);
        } else if (user instanceof Employee employee) {
            return EmployeeProfileResponse.fromEntity(employee);
        }

        log.error("Unknown user entity subtype detected for email: '{}'", userEmail);
        throw new IllegalStateException(
                messageSource.getMessage("error.user.unknown.type", null, getLocale())
        );
    }

    @Override
    @Transactional
    public void updateUser(String userEmail, UserProfileUpdateRequest request) {
        log.info("Updating user profile for email: '{}'", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("Failed to update profile: User '{}' not found", userEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{userEmail}, getLocale())
                    );
                });

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.middleName() != null) user.setMiddleName(request.middleName());
        if (request.birthday() != null) user.setBirthday(request.birthday());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.info("Admin privileges applied during user update for '{}'", userEmail);
            if (request.role() != null) user.setRole(request.role());
            if (user instanceof Client client && request.card() != null) {
                client.setCart(request.card());
            }
            if (user instanceof Employee employee && request.department() != null) {
                employee.setDepartment(request.department());
            }
            if (request.enable() != null) user.setEnabled(request.enable());
        }

        userRepository.save(user);
        log.info("User profile successfully updated for email: '{}'", userEmail);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        log.info("Password reset requested for email: '{}'", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset rejected: User '{}' not found", email);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{email}, getLocale())
                    );
                });

        String token = verificationTokenService.generateToken(user, TokenAction.RESET_PASSWORD, 60);
        String resetUrl = BASE_URL + "/reset-password?token=" + token;

        mailService.sendPasswordResetMessage(email, resetUrl);
        log.info("Password reset email sent to: '{}'", email);
    }

    @Override
    @Transactional
    public void updatePassword(String currentEmail, UserPasswordChangeRequest request) {
        log.info("Attempting password update for user: '{}'", currentEmail);

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> {
                    log.warn("Password update rejected: User '{}' not found", currentEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{currentEmail}, getLocale())
                    );
                });

        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Password update failed for '{}': New passwords do not match", currentEmail);
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.password.mismatch", null, getLocale())
            );
        }

        if (!passwordEncoder.matches(request.oldPassword(), currentUser.getPassword())) {
            log.warn("Password update failed for '{}': Old password incorrect", currentEmail);
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.password.old.mismatch", null, getLocale())
            );
        }

        if (passwordEncoder.matches(request.newPassword(), currentUser.getPassword())) {
            log.warn("Password update failed for '{}': New password cannot be the same as old", currentEmail);
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.password.new.same", null, getLocale())
            );
        }

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
        log.info("Password successfully changed for user: '{}'", currentEmail);
    }

    @Override
    @Transactional
    public void resetPassword(String token, UserPasswordResetRequest request) {
        log.info("Attempting password reset via token");

        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Reset password failed: Confirmation password does not match");
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.password.mismatch", null, getLocale())
            );
        }

        User user = verificationTokenService.validateToken(token)
                .orElseThrow(() -> {
                    log.warn("Reset password failed: Invalid or expired token");
                    return new NotFoundException(
                            messageSource.getMessage("error.token.not.found", new Object[]{token}, getLocale())
                    );
                });

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password successfully reset for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public void updateEmail(String currentEmail, UserEmailChangeRequest request) {
        log.info("Attempting email change from '{}' to '{}'", currentEmail, request.newEmail());

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> {
                    log.warn("Email change rejected: User '{}' not found", currentEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{currentEmail}, getLocale())
                    );
                });

        if (!passwordEncoder.matches(request.password(), currentUser.getPassword())) {
            log.warn("Email change failed for '{}': Incorrect password provided", currentEmail);
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.password.incorrect", null, getLocale())
            );
        }

        if (userRepository.findByEmail(request.newEmail()).isPresent()) {
            log.warn("Email change failed for '{}': Target email '{}' is already taken", currentEmail, request.newEmail());
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.user.email.exists", new Object[]{request.newEmail()}, getLocale())
            );
        }

        currentUser.setEmail(request.newEmail());
        userRepository.save(currentUser);
        log.info("Email successfully updated: '{}' -> '{}'", currentEmail, request.newEmail());
    }

    @Override
    @Transactional
    public void verifyAccount(String token) {
        log.info("Attempting account verification via token");

        User verificationUser = verificationTokenService.validateToken(token)
                .orElseThrow(() -> {
                    log.warn("Account verification failed: Invalid or expired token");
                    return new NotFoundException(
                            messageSource.getMessage("error.user.token.not.valid", null, getLocale())
                    );
                });

        verificationUser.setEnabled(true);
        userRepository.save(verificationUser);
        log.info("Account successfully activated for user id: {}, email: '{}'",
                verificationUser.getId(), verificationUser.getEmail());
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        log.info("Attempting to delete user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to delete: User id {} not found", id);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.id.not.found", new Object[]{id}, getLocale())
                    );
                });

        userRepository.delete(user);
        log.info("User with id {} successfully deleted", id);
    }

    @Override
    @Transactional
    public void deleteUserByEmail(String email) {
        log.info("Attempting to delete user by email: '{}'", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        if (user instanceof Client client) {
            log.debug("Deleting shopping cart for client id: {}", client.getId());
            cartRepository.deleteByClient(client);
        }

        if (verificationTokenRepository != null) {
            verificationTokenRepository.deleteByUserId(user.getId());
        }

        userRepository.delete(user);
        log.info("User with email '{}' and id {} deleted successfully", email, user.getId());
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}