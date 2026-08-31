package com.epam.rd.autocode.assessment.appliances.dto.employee;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        String firstName,
        String lastName,
        String middleName,
        String email,
        String password,
        LocalDate birthday,
        String department
) {
}
