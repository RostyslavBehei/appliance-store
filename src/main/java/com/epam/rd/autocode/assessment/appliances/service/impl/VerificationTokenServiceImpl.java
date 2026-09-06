package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.VerificationToken;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import com.epam.rd.autocode.assessment.appliances.repository.VerificationTokenRepository;
import com.epam.rd.autocode.assessment.appliances.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public String generateToken(User user, TokenAction tokenAction, int expirationMinutes) {
        log.info("Generating verification token for user id: {}, action: {}, expiration: {} mins",
                user.getId(), tokenAction, expirationMinutes);

        verificationTokenRepository.deleteByUserAndTokenAction(user, tokenAction);
        String token;

        do {
            token = UUID.randomUUID().toString();
        } while (verificationTokenRepository.existsByToken(token));

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .tokenAction(tokenAction)
                .expiryDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        verificationTokenRepository.save(verificationToken);
        log.info("Token successfully created and saved for user id: {} (token prefix: '{}')",
                user.getId(), maskToken(token));

        return token;
    }

    @Override
    @Transactional
    public Optional<User> validateToken(String token) {
        log.info("Validating token (prefix: '{}')", maskToken(token));

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Token validation failed: Token '{}' not found in database", maskToken(token));
                    return new NotFoundException(
                            messageSource.getMessage("error.token.not.found", new Object[]{token}, getLocale())
                    );
                });

        if (verificationToken.isExpired()) {
            log.warn("Token validation failed: Token for user id: {} is expired (expiryDate: {})",
                    verificationToken.getUser().getId(), verificationToken.getExpiryDate());
            return Optional.empty();
        }

        User user = verificationToken.getUser();
        verificationTokenRepository.delete(verificationToken);
        log.info("Token (prefix: '{}') consumed successfully for user id: {}", maskToken(token), user.getId());

        return Optional.of(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token, TokenAction tokenAction) {
        log.debug("Checking token validity for action: {}, token prefix: '{}'", tokenAction, maskToken(token));

        VerificationToken verificationToken = verificationTokenRepository.findByTokenAndTokenAction(token, tokenAction)
                .orElseThrow(() -> {
                    log.warn("Token check failed: Token not found for action: {}", tokenAction);
                    return new NotFoundException(
                            messageSource.getMessage("error.token.not.found", new Object[]{token}, getLocale())
                    );
                });

        boolean valid = !verificationToken.isExpired();
        log.debug("Token check result for user id: {}: isValid={}", verificationToken.getUser().getId(), valid);
        return valid;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 8) + "-****";
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}