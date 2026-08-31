package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeSummaryResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDate birthday,
        Role role,
        boolean enable,
        String department,
        LocalDateTime createdAt
) {

    public static EmployeeSummaryResponse fromEntity(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getBirthday(),
                employee.getRole(),
                employee.getEnabled(),
                employee.getDepartment(),
                employee.getCreatedAt()
        );
    }
}
