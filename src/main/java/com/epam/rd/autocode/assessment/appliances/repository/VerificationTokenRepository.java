package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.VerificationToken;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByTokenAndTokenAction(String token, TokenAction tokenAction);

    boolean existsByToken(String token);

    void deleteByUserId(Long userId);
    void deleteByUserAndTokenAction(User user, TokenAction tokenAction);
}
