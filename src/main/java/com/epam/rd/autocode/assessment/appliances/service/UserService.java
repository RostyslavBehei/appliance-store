package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.user.*;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserProfileResponse getUserByEmail(String userEmail);
    void updateUser(String userEmail, UserProfileUpdateRequest request);
    void forgotPassword(String email);
    void updatePassword(String currentEmail, UserPasswordChangeRequest request);
    void resetPassword(String token, UserPasswordResetRequest request);
    void updateEmail(String currentEmail, UserEmailChangeRequest request);
    void verifyAccount(String token);
    void deleteUserById(Long id);
    void deleteUserByEmail(String userEmail);
}
