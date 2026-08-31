package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest (
        @NotBlank(message = "{user.email.notBlank}")
        @Email(message = "{user.email.invalid}")
        String email,

        @NotBlank(message = "{user.password.notBlank}")
        @Size(min = 8, max = 24, message = "{user.password.size}")
        String password
) {

    public static UserLoginRequest empty() {
        return new UserLoginRequest(null, null);
    }
}
