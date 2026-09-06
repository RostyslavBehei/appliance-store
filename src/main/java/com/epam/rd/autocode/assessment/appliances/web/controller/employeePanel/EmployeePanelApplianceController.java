package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
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
@RequestMapping("/employee-panel/appliances")
@RequiredArgsConstructor
public class EmployeePanelApplianceController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String showAppliancesPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "sort", defaultValue = "newest", required = false) String sort,
            Model model) {

        int currentPage = Math.max(page, 0);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(currentPage, size, sorting);

        log.debug("Employee fetching appliances list: page={}, size={}, keyword='{}', sort='{}'",
                currentPage, size, keyword, sort);

        Page<ApplianceSummaryResponse> response = applianceService.getAllAppliance(
                keyword, null, null, null, null, null, null, pageable
        );

        model.addAttribute("appliances", response);
        model.addAttribute("activePage", "appliances");

        return "employee-panel/appliance/employee-appliances-page";
    }

    @GetMapping("/new")
    public String showNewApplianceFormPage(Model model) {
        log.debug("Employee opening new appliance form");

        model.addAttribute("applianceRequest", new ApplianceUpdateRequest(null, null, null, null, null, null, null, null, null, null));
        populateFormData(model);
        return "employee-panel/appliance/employee-appliance-form-page";
    }

    @PostMapping("/new")
    public String processNewAppliance(
            @Valid @ModelAttribute("applianceRequest") ApplianceUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        String employee = principal != null ? principal.getName() : "EMPLOYEE";

        if (bindingResult.hasErrors()) {
            log.warn("Employee '{}' submitted invalid appliance form: {} error(s)", employee, bindingResult.getErrorCount());
            populateFormData(model);
            return "employee-panel/appliance/employee-appliance-form-page";
        }

        log.info("Employee '{}' saving appliance model='{}'", employee, request.model());
        applianceService.updateAppliance(request);
        log.info("Appliance successfully saved by employee '{}'", employee);

        return "redirect:/employee-panel/appliances";
    }

    @PostMapping("/{id}/delete")
    public String processDeleteAppliance(@PathVariable Long id, Principal principal) {
        String employee = principal != null ? principal.getName() : "EMPLOYEE";
        log.info("Employee '{}' deleting appliance id: {}", employee, id);

        applianceService.deleteApplianceById(id);
        log.info("Appliance id: {} deleted successfully by employee '{}'", id, employee);

        return "redirect:/employee-panel/appliances";
    }

    private void populateFormData(Model model) {
        model.addAttribute("manufacturers", manufacturerService.getAllManufacturer());
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("activePage", "appliances");
    }
}