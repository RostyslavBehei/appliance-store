package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserEmailChangeRequest(
        @NotBlank(message = "{user.email.change.newEmail.notBlank}")
        @Email(message = "{user.email.change.newEmail.email}")
        String newEmail,

        @NotBlank(message = "{user.email.change.password.notBlank}")
        @Size(min = 8, max = 24, message = "{user.email.change.password.size}")
        String password
) {
}
