package com.epam.rd.autocode.assessment.appliances.web.controller.admin;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UserService userService;
    private final ClientService clientService;
    private final EmployeeService employeeService;

    @GetMapping
    public String showUsersPage(Model model) {
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

        Sort sorting = switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "firstName");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<ClientSummaryResponse> clientsPage = clientService.getClientsPage(keyword, pageable);

        model.addAttribute("clientsPage", clientsPage);
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

        Sort sorting = switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "firstName");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<EmployeeSummaryResponse> employeesPage = employeeService.getEmployeesPage(keyword, pageable);

        model.addAttribute("employeesPage", employeesPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("activePage", "users");

        return "admin/user/admin-users-employees-page";
    }

    @GetMapping("/clients/new")
    public String showNewClientPage(Model model) {

        model.addAttribute("client", ClientProfileResponse.empty());

        return "admin/user/admin-users-clients-form-page";

    }

    @GetMapping("/clients/edit/{id}")
    public String showEditClientPage(
            @PathVariable Long id,
            Model model
    ) {

        ClientProfileResponse response = clientService.getClientById(id);

        model.addAttribute("client", response);
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-clients-form-page";
    }

    @GetMapping("/employees/edit/{id}")
    public String showEditEmployeesPage(
            @PathVariable Long id,
            Model model
    ) {

        EmployeeProfileResponse response = employeeService.getEmployeeById(id);

        model.addAttribute("employee", response);
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-employees-form-page";
    }

    @GetMapping("/employees/new")
    public String showNewEmployeesPage(Model model) {

        model.addAttribute("employee", EmployeeProfileResponse.empty());
        model.addAttribute("activePage", "users");
        return "admin/user/admin-users-employees-form-page";
    }

    @PostMapping("/clients/save")
    public String processEditClient(
            @ModelAttribute ClientUpdateRequest request
    ) {

        clientService.saveClient(request);

        return "redirect:/admin/users/clients";
    }

    @PostMapping("/employees/save")
    public String processEditEmployee(
            @ModelAttribute EmployeeUpdateRequest request) {
        employeeService.saveEmployee(request);

        return "redirect:/admin/users/employees";
    }

    @PostMapping("/delete/{id}")
    public String processDeleteUser(
            @PathVariable Long id) {

        userService.deleteUserById(id);

        return "redirect:/admin/users";
    }
}
