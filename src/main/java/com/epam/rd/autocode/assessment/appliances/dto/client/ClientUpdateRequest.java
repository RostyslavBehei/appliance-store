package com.epam.rd.autocode.assessment.appliances.dto.client;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ClientUpdateRequest(
        Long id,
        String firstName,
        String lastName,
        String middleName,
        String email,
        String password,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate birthday,
        String cart,
        boolean enabled
) {
}
