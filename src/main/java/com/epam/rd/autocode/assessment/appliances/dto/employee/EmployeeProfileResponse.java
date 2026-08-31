package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileResponse;
import com.epam.rd.autocode.assessment.appliances.model.Employee;

import java.time.LocalDate;

public record EmployeeProfileResponse (
        Long id,
        String firstName,
        String lastName,
        String middleName,
        String email,
        LocalDate birthday,
        String role,
        String department,
        Boolean enabled
) implements UserProfileResponse {

    public static EmployeeProfileResponse fromEntity(Employee employee) {
        return new EmployeeProfileResponse (
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getMiddleName(),
                employee.getEmail(),
                employee.getBirthday(),
                employee.getRole().name(),
                employee.getDepartment(),
                employee.getEnabled()
        );
    }

    public static EmployeeProfileResponse empty() {
        return new EmployeeProfileResponse(null, null, null, null, null, null, null, null, false);
    }
}
