package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UserService userService;
    private final ClientService clientService;
    private final EmployeeService employeeService;

    @GetMapping
    public String showUsersPage(Model model) {
        log.debug("Rendering admin users overview page");

        Pageable pageable = PageRequest.of(0, 10);
        Page<ClientSummaryResponse> clients = clientService.getAllClients(pageable);
        Page<EmployeeSummaryResponse> employees = employeeService.getAllEmployees(pageable);

        model.addAttribute("clients", clients);
        model.addAttribute("employees", employees);
        model.addAttribute("activePage", "users");

        return "admin/user/admin-users-page";
    }

    @GetMapping("/clients")
    public String showClientsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        int currentPage = Math.max(page, 0);
        Sort sorting = switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "firstName");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(currentPage, size, sorting);
        log.debug("Fetching clients list: page={}, keyword='{}', sort='{}'", currentPage, keyword, sort);

        Page<ClientSummaryResponse> clientsPage = clientService.getClientsPage(keyword, pageable);

        model.addAttribute("clientsPage", clientsPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("activePage", "users");

        return "admin/user/admin-users-clients-page";
    }

    @GetMapping("/employees")
    public String showEmployeesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        int currentPage = Math.max(page, 0);
        Sort sorting = switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "firstName");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(currentPage, size, sorting);
        log.debug("Fetching employees list: page={}, keyword='{}', sort='{}'", currentPage, keyword, sort);

        Page<EmployeeSummaryResponse> employeesPage = employeeService.getEmployeesPage(keyword, pageable);
        model.addAttribute("employeesPage", employeesPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("activePage", "users");

        return "admin/user/admin-users-employees-page";
    }

    @GetMapping("/clients/new")
    public String showNewClientPage(Model model) {
        log.debug("Opening new client creation form");
        model.addAttribute("client", ClientProfileResponse.empty());
        return "admin/user/admin-users-clients-form-page";
    }

    @GetMapping("/clients/edit/{id}")
    public String showEditClientPage(@PathVariable Long id, Model model) {
        log.debug("Opening edit form for client id: {}", id);
        ClientProfileResponse response = clientService.getClientById(id);
        model.addAttribute("client", response);
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-clients-form-page";
    }

    @GetMapping("/employees/edit/{id}")
    public String showEditEmployeesPage(@PathVariable Long id, Model model) {
        log.debug("Opening edit form for employee id: {}", id);
        EmployeeProfileResponse response = employeeService.getEmployeeById(id);
        model.addAttribute("employee", response);
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-employees-form-page";
    }

    @GetMapping("/employees/new")
    public String showNewEmployeesPage(Model model) {
        log.debug("Opening new employee creation form");
        model.addAttribute("employee", EmployeeProfileResponse.empty());
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-employees-form-page";
    }

    @PostMapping("/clients/save")
    public String processEditClient(
            @Valid @ModelAttribute("client") ClientUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        String admin = principal != null ? principal.getName() : "ADMIN";

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed saving client email '{}': {} error(s)",
                    request.email(), bindingResult.getErrorCount());
            model.addAttribute("activePage", "users");
            return "admin/user/admin-users-clients-form-page";
        }

        log.info("Admin '{}' saving client (id={}, email='{}')", admin, request.id(), request.email());
        clientService.saveClient(request);
        log.info("Client '{}' successfully saved by admin '{}'", request.email(), admin);

        return "redirect:/admin/users/clients";
    }

    @PostMapping("/employees/save")
    public String processEditEmployee(
            @Valid @ModelAttribute("employee") EmployeeUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        String admin = principal != null ? principal.getName() : "ADMIN";

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed saving employee email '{}': {} error(s)",
                    request.email(), bindingResult.getErrorCount());
            model.addAttribute("activePage", "users");
            return "admin/user/admin-users-employees-form-page";
        }

        log.info("Admin '{}' saving employee (id={}, email='{}')", admin, request.id(), request.email());
        employeeService.saveEmployee(request);
        log.info("Employee '{}' successfully saved by admin '{}'", request.email(), admin);

        return "redirect:/admin/users/employees";
    }

    @PostMapping("/delete/{id}")
    public String processDeleteUser(@PathVariable Long id, Principal principal) {
        String admin = principal != null ? principal.getName() : "ADMIN";
        log.info("Admin '{}' deleting user id: {}", admin, id);

        userService.deleteUserById(id);
        log.info("User id: {} deleted successfully by admin '{}'", id, admin);

        return "redirect:/admin/users";
    }
}