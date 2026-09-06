package com.epam.rd.autocode.assessment.appliances.web.controller.employeePanel;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/employee-panel/manufacturers")
@RequiredArgsConstructor
public class EmployeePanelManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public String showManufacturersPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model
    ) {
        log.debug("Employee rendering manufacturers list: keyword='{}'", keyword);

        List<ManufacturerResponse> response = manufacturerService.getAllManufacturer(keyword);

        model.addAttribute("manufacturers", response);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "manufacturers");

        return "employee-panel/manufacturer/employee-manufacturer-page";
    }

    @PostMapping
    public String processAddManufacturer(
            @RequestParam("name") String name,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String employee = principal != null ? principal.getName() : "EMPLOYEE";

        if (name == null || name.trim().isBlank()) {
            log.warn("Employee '{}' tried to add manufacturer with empty name", employee);
            redirectAttributes.addFlashAttribute("errorMessage", "Manufacturer name cannot be empty");
            return "redirect:/employee-panel/manufacturers";
        }

        try {
            log.info("Employee '{}' creating manufacturer with name='{}'", employee, name.trim());
            manufacturerService.createManufacturer(new ManufacturerCreateRequest(name.trim()));
            log.info("Manufacturer '{}' successfully created by employee '{}'", name.trim(), employee);
        } catch (Exception ex) {
            log.warn("Failed to create manufacturer '{}' by employee '{}': {}", name, employee, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/employee-panel/manufacturers";
    }

    @PostMapping("/{id}/edit")
    public String processEditManufacturer(
            @PathVariable Long id,
            @RequestParam("name") String name,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String employee = principal != null ? principal.getName() : "EMPLOYEE";

        if (name == null || name.trim().isBlank()) {
            log.warn("Employee '{}' tried to update manufacturer id={} with empty name", employee, id);
            redirectAttributes.addFlashAttribute("errorMessage", "Manufacturer name cannot be empty");
            return "redirect:/employee-panel/manufacturers";
        }

        try {
            log.info("Employee '{}' updating manufacturer id={} to new name='{}'", employee, id, name.trim());
            manufacturerService.updateManufacturer(new ManufacturerUpdateRequest(id, name.trim()));
            log.info("Manufacturer id={} successfully updated by employee '{}'", id, employee);
        } catch (Exception ex) {
            log.warn("Failed to update manufacturer id={} by employee '{}': {}", id, employee, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/employee-panel/manufacturers";
    }

    @PostMapping("/{id}/delete")
    public String deleteManufacturer(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String employee = principal != null ? principal.getName() : "EMPLOYEE";
        log.info("Employee '{}' attempting to delete manufacturer id: {}", employee, id);

        try {
            manufacturerService.deleteManufacturerById(id);
            log.info("Manufacturer id: {} deleted successfully by employee '{}'", id, employee);
        } catch (Exception ex) {
            log.error("Failed to delete manufacturer id: {} by employee '{}': {}", id, employee, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete manufacturer: it may be linked to existing appliances.");
        }
        return "redirect:/employee-panel/manufacturers";
    }
}