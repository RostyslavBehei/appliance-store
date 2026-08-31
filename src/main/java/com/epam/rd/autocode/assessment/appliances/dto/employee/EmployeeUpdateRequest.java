package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EmployeeUpdateRequest (
        Long id,
        String firstName,
        String lastName,
        String middleName,
        String email,
        String password,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate birthday,
        String department,
        Role role,
        boolean enabled
) {
}
