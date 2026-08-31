package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public EmployeeProfileResponse getEmployeeById(Long id) {
        return EmployeeProfileResponse.fromEntity(employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee with id " + id + " not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(EmployeeSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getEmployeesPage(String keyword, Pageable pageable) {
        Page<Employee> employees;

        if (keyword != null && !keyword.trim().isEmpty()) {
            employees = employeeRepository.searchEmployees(keyword, pageable);
        } else {
            employees = employeeRepository.findAll(pageable);
        }

        return employees.map(EmployeeSummaryResponse::fromEntity);
    }

    @Override
    @Transactional
    public void saveEmployee(EmployeeUpdateRequest request) {
        Employee employee;

        if (request.id() != null) {
            employee = employeeRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundException("Employee with id " + request.id() + " not found"));

            if (!employee.getEmail().equals(request.email())) {
                if (userRepository.findByEmail(request.email()).isPresent()) {
                    throw new AlreadyExistsException("User with email " + request.email() + " already exists");
                }
            }
        } else {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new AlreadyExistsException("User with email " + request.email() + " already exists");
            }
            employee = new Employee();
        }

        if (request.firstName() != null) employee.setFirstName(request.firstName());
        if (request.lastName() != null) employee.setLastName(request.lastName());
        if (request.middleName() != null) employee.setMiddleName(request.middleName());
        if (request.email() != null) employee.setEmail(request.email());
        if (request.birthday() != null) employee.setBirthday(request.birthday());
        if (request.department() != null) employee.setDepartment(request.department());

        employee.setRole(request.role() != null ? request.role() : Role.ROLE_EMPLOYEE);
        employee.setEnabled(request.enabled());

        if (request.password() != null && !request.password().isBlank()) {
            employee.setPassword(passwordEncoder.encode(request.password()));
        }

        employeeRepository.save(employee);
    }
}
