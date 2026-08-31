package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.Size;

public record UserPasswordChangeRequest(

        @Size(min = 8, max = 24, message = "{user.oldPassword.size}")
        String oldPassword,

        @Size(min = 8, max = 24, message = "{user.newPassword.size}")
        String newPassword,

        @Size(min = 8, max = 24, message = "{user.confirmPassword.size}")
        String confirmPassword
) {
}
