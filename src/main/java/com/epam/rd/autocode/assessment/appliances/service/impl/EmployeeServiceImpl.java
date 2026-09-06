package com.epam.rd.autocode.assessment.appliances.service.impl;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileResponse getEmployeeById(Long id) {
        log.debug("Fetching employee with id '{}'", id);
        return EmployeeProfileResponse.fromEntity(employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found with id: '{}'", id);
                    return new NotFoundException(messageSource.getMessage("error.employee.not.found", new Object[]{id}, getLocale()));
                }));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getAllEmployees(Pageable pageable) {
        log.debug("Fetching all employees with pagination: pageNumber='{}', pageSize='{}'", pageable.getPageNumber(), pageable.getPageSize());
        return employeeRepository.findAll(pageable)
                .map(EmployeeSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> getEmployeesPage(String keyword, Pageable pageable) {
        log.debug("Searching employee page: keyword='{}', pageNumber={}", keyword, pageable.getPageNumber());

        Page<Employee> employees;
        if (keyword == null || keyword.trim().isBlank()) {
            employees = employeeRepository.findAll(pageable);
        } else {
            employees = employeeRepository.searchEmployees(keyword.trim(), pageable);
        }
        return employees.map(EmployeeSummaryResponse::fromEntity);
    }

    @Override
    @Transactional
    public void saveEmployee(EmployeeUpdateRequest request) {
        boolean isNew = request.id() == null;
        Employee employee;

        if (!isNew) {
            log.info("Updating employee with id: '{}'", request.id());
            employee = employeeRepository.findById(request.id())
                    .orElseThrow(() -> {
                        log.warn("Failed to update: Employee not found with id: '{}'", request.id());
                        return new NotFoundException(messageSource.getMessage("error.employee.not.found", new Object[]{request.id()}, getLocale()));
                    });

            if (!employee.getEmail().equals(request.email())) {
                if (userRepository.findByEmail(request.email()).isPresent()) {
                    log.warn("Failed to update: Employee email '{}' is already in use", request.email());
                    throw new AlreadyExistsException(
                            messageSource.getMessage("error.user.email.exists", new Object[]{request.email()}, getLocale())
                    );
                }
            }
        } else {
            log.info("Creating employee vie admin panel with email: '{}'", request.email());
            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Failed to update: Employee email '{}' is already in use", request.email());
                throw new AlreadyExistsException(
                        messageSource.getMessage("error.user.email.exists", new Object[]{request.email()}, getLocale())
                );
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
        log.info("Employee successfully {} with id: '{}', email: '{}', enabled: '{}'", isNew, employee.getId(), employee.getEmail(), employee.getEnabled());
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}