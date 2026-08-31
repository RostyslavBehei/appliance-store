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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
            return "redirect:/login";
        }

        ApplianceResponse appliance = applianceService.getApplianceById(id);
        List<ManufacturerResponse> manufacturers = manufacturerService.getAllManufacturer();
        List<Category> categories = Category.getCategories();
        List<PowerType> powerTypes = PowerType.getPowerTypes();

        model.addAttribute("appliance", appliance);
        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("categories", categories);
        model.addAttribute("powerTypes", powerTypes);

        return "appliance/appliance-edit-page";
    }

    @PostMapping("/edit")
    public String processEditAppliance(
            @Valid @ModelAttribute ApplianceUpdateRequest request,
             Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        applianceService.updateAppliance(request);

        return "redirect:/appliances/" + request.id();
    }

    @PostMapping("/delete/{id}")
    public String deleteAppliance(
            @PathVariable Long id,
            Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        applianceService.deleteApplianceById(id);
        return "redirect:/";
    }
}

