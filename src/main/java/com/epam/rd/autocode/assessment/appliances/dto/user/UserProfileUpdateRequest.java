package com.epam.rd.autocode.assessment.appliances.dto.user;

import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UserProfileUpdateRequest(
        String firstName,
        String lastName,
        String middleName,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate birthday,
        Role role,
        String card,
        String department,
        Boolean enable
) {
}
