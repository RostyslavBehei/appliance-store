package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee testEmployee;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("employee@test.com");
        testEmployee.setFirstName("John");
        testEmployee.setRole(Role.ROLE_EMPLOYEE);

        pageable = PageRequest.of(0, 10);

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Get Employee By Id - Should return EmployeeProfileResponse")
    void getEmployeeById_ShouldReturnEmployee() {
        Long id = 1L;
        when(employeeRepository.findById(id)).thenReturn(Optional.of(testEmployee));

        EmployeeProfileResponse result = employeeService.getEmployeeById(id);

        assertNotNull(result);
        assertEquals("employee@test.com", result.email());
        verify(employeeRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get Employee By Id - Should throw NotFoundException if employee not exist")
    void getEmployeeById_ShouldThrowNotFoundExceptionIfNotExist() {
        Long id = 99L;
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> employeeService.getEmployeeById(id));

        assertEquals("error.employee.not.found", result.getMessage());
        verify(employeeRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get All Employees - Should Return All Employees")
    void getAllEmployees_ShouldReturnAllEmployees() {
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));
        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);

        Page<EmployeeSummaryResponse> result = employeeService.getAllEmployees(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("employee@test.com", result.getContent().get(0).email());
        verify(employeeRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get Employees Page - Should search by keyword")
    void getEmployeesPage_ShouldSearchByKeyword() {
        String keyword = "John";
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));

        when(employeeRepository.searchEmployees(keyword, pageable)).thenReturn(employeePage);

        Page<EmployeeSummaryResponse> result = employeeService.getEmployeesPage(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(employeeRepository, times(1)).searchEmployees(keyword, pageable);
    }

    @Test
    @DisplayName("Get Employees Page - Should return all if keyword is null or empty")
    void getEmployeesPage_ShouldReturnAllIfKeywordNullOrEmpty() {
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee));

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);

        Page<EmployeeSummaryResponse> resultNull = employeeService.getEmployeesPage(null, pageable);
        Page<EmployeeSummaryResponse> resultEmpty = employeeService.getEmployeesPage("   ", pageable);

        assertEquals(1, resultNull.getTotalElements());
        assertEquals(1, resultEmpty.getTotalElements());
        verify(employeeRepository, times(2)).findAll(pageable);
        verify(employeeRepository, never()).searchEmployees(anyString(), any());
    }

    @Test
    @DisplayName("Save Employee - Should successfully update existing employee")
    void saveEmployee_ShouldUpdateExistingEmployee() {
        EmployeeUpdateRequest request = mock(EmployeeUpdateRequest.class);
        when(request.id()).thenReturn(1L);
        when(request.email()).thenReturn("employee@test.com");
        when(request.firstName()).thenReturn("UpdatedName");
        when(request.password()).thenReturn("newPass");
        when(request.enabled()).thenReturn(true);
        when(request.role()).thenReturn(Role.ROLE_ADMIN);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");

        employeeService.saveEmployee(request);

        assertEquals("UpdatedName", testEmployee.getFirstName());
        assertEquals("encodedPass", testEmployee.getPassword());
        assertEquals(Role.ROLE_ADMIN, testEmployee.getRole());
        verify(employeeRepository, times(1)).save(testEmployee);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Save Employee - Should throw AlreadyExistsException if new email is taken during update")
    void saveEmployee_ShouldThrowAlreadyExistsExceptionIfEmailTakenOnUpdate() {
        EmployeeUpdateRequest request = mock(EmployeeUpdateRequest.class);
        when(request.id()).thenReturn(1L);
        when(request.email()).thenReturn("taken@test.com");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> employeeService.saveEmployee(request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Save Employee - Should successfully create new employee")
    void saveEmployee_ShouldCreateNewEmployee() {
        EmployeeUpdateRequest request = mock(EmployeeUpdateRequest.class);
        when(request.id()).thenReturn(null);
        when(request.email()).thenReturn("new@test.com");
        when(request.enabled()).thenReturn(true);
        when(request.role()).thenReturn(null);

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        employeeService.saveEmployee(request);

        verify(employeeRepository, times(1)).save(argThat(employee ->
                employee.getEmail().equals("new@test.com") &&
                        employee.getRole() == Role.ROLE_EMPLOYEE &&
                        employee.getEnabled() == true
        ));
    }

    @Test
    @DisplayName("Save Employee - Should throw AlreadyExistsException if email taken during creation")
    void saveEmployee_ShouldThrowAlreadyExistsExceptionIfEmailTakenOnCreate() {
        EmployeeUpdateRequest request = mock(EmployeeUpdateRequest.class);
        when(request.id()).thenReturn(null);
        when(request.email()).thenReturn("taken@test.com");

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> employeeService.saveEmployee(request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Save Employee - Should throw NotFoundException if updating non-existing employee")
    void saveEmployee_ShouldThrowNotFoundExceptionIfEmployeeNotExistOnUpdate() {
        EmployeeUpdateRequest request = mock(EmployeeUpdateRequest.class);
        when(request.id()).thenReturn(99L);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> employeeService.saveEmployee(request));

        assertEquals("error.employee.not.found", result.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }
}