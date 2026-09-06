package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface VerificationTokenService {
    String generateToken(User user, TokenAction tokenAction, int expirationMinutes);

    Optional<User> validateToken(String token);
    boolean isTokenValid(String token, TokenAction tokenAction);
}
