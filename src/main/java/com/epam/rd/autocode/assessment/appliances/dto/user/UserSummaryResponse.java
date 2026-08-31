package com.epam.rd.autocode.assessment.appliances.dto.user;

import com.epam.rd.autocode.assessment.appliances.model.User;

import java.time.LocalDate;

public record UserSummaryResponse(
        String firstName,
        String lastName,
        String middleName,
        LocalDate birthday,
        String role,
        Boolean enable
) {

    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                user.getBirthday(),
                user.getRole().name(),
                user.getEnabled()
        );
    }
}
