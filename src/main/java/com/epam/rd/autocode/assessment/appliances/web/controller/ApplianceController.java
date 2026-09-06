package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/appliances")
@RequiredArgsConstructor
public class ApplianceController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping("/{id}")
    public String showAppliancePage(
            @PathVariable Long id,
            Model model) {
        log.debug("Displaying appliance details page for id: {}", id);

        ApplianceResponse response = applianceService.getApplianceById(id);
        model.addAttribute("appliance", response);

        return "appliance/appliance-details";
    }

    @GetMapping("/edit/{id}")
    public String showEditAppliancePage(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        if (principal == null) {
            log.warn("Unauthorized attempt to open appliance edit page for id: {}", id);
            return "redirect:/login";
        }

        log.debug("User '{}' opened edit page for appliance id: {}", principal.getName(), id);

        ApplianceResponse appliance = applianceService.getApplianceById(id);
        populateFormData(model);
        model.addAttribute("appliance", appliance);

        return "appliance/appliance-edit-page";
    }

    @PostMapping("/edit")
    public String processEditAppliance(
            @Valid @ModelAttribute("appliance") ApplianceUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        if (principal == null) {
            log.warn("Unauthorized attempt to submit appliance update for id: {}", request.id());
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed for appliance update id={}: {} error(s)",
                    request.id(), bindingResult.getErrorCount());
            populateFormData(model);
            return "appliance/appliance-edit-page";
        }

        log.info("User '{}' updated appliance id={}", principal.getName(), request.id());
        applianceService.updateAppliance(request);

        return "redirect:/appliances/" + request.id();
    }

    @PostMapping("/delete/{id}")
    public String deleteAppliance(
            @PathVariable Long id,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthorized attempt to delete appliance id: {}", id);
            return "redirect:/login";
        }

        log.info("User '{}' deleted appliance id: {}", principal.getName(), id);
        applianceService.deleteApplianceById(id);

        return "redirect:/";
    }

    private void populateFormData(Model model) {
        List<ManufacturerResponse> manufacturers = manufacturerService.getAllManufacturer();
        List<Category> categories = Category.getCategories();
        List<PowerType> powerTypes = PowerType.getPowerTypes();

        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("categories", categories);
        model.addAttribute("powerTypes", powerTypes);
    }
}