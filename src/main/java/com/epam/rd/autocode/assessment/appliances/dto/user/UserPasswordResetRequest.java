package com.epam.rd.autocode.assessment.appliances.dto.user;

public record UserPasswordResetRequest(
        String newPassword,
        String confirmPassword
) {
}
