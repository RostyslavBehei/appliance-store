package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface EmployeeService {
    EmployeeProfileResponse getEmployeeById(Long id);
    Page<EmployeeSummaryResponse> getAllEmployees(Pageable pageable);
    Page<EmployeeSummaryResponse> getEmployeesPage(String keyword, Pageable pageable);

    void saveEmployee(EmployeeUpdateRequest request);
}
