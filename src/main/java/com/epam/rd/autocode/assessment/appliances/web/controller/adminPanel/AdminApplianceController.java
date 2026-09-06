package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
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
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/appliances")
@RequiredArgsConstructor
public class AdminApplianceController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String showAppliancePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            Model model
    ) {
        int currentPage = Math.max(page, 0);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(currentPage, size, sorting);

        log.debug("Rendering admin appliances page: page={}, size={}, keyword='{}', sort='{}'",
                currentPage, size, keyword, sort);

        Page<ApplianceSummaryResponse> appliancesResponse =
                applianceService.getAllAppliance(keyword, null, null, null, null, null, null, pageable);

        model.addAttribute("activePage", "appliances");
        model.addAttribute("keyword", keyword);
        model.addAttribute("appliances", appliancesResponse);

        return "admin/appliance/admin-appliances-page";
    }

    @GetMapping("/{id}")
    public String showApplianceDetailPage(@PathVariable Long id, Model model) {
        log.debug("Viewing admin appliance details for id: {}", id);

        ApplianceResponse applianceResponse = applianceService.getApplianceById(id);
        model.addAttribute("activePage", "appliances");
        model.addAttribute("appliance", applianceResponse);

        return "admin/appliance/admin-appliances-details-page";
    }

    @GetMapping("/edit/{id}")
    public String showEditAppliancePage(@PathVariable Long id, Model model) {
        log.debug("Opening edit form for appliance id: {}", id);

        ApplianceResponse applianceResponse = applianceService.getApplianceById(id);
        populateFormData(model);
        model.addAttribute("appliance", applianceResponse);

        return "admin/appliance/admin-appliances-form-page";
    }

    @GetMapping("/new")
    public String showNewAppliancePage(Model model) {
        log.debug("Opening new appliance creation form");

        populateFormData(model);
        model.addAttribute("appliance", ApplianceResponse.empty());

        return "admin/appliance/admin-appliances-form-page";
    }

    @PostMapping("/save")
    public String processSaveAppliance(
            @Valid @ModelAttribute("appliance") ApplianceUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        String admin = principal != null ? principal.getName() : "ADMIN";

        if (bindingResult.hasErrors()) {
            log.warn("Admin '{}' failed validation saving appliance id={}: {} error(s)",
                    admin, request.id(), bindingResult.getErrorCount());
            populateFormData(model);
            return "admin/appliance/admin-appliances-form-page";
        }

        log.info("Admin '{}' saving appliance (id={}, model='{}')", admin, request.id(), request.model());
        applianceService.updateAppliance(request);
        log.info("Appliance successfully saved by admin '{}'", admin);

        return "redirect:/admin/appliances";
    }

    @PostMapping("/delete/{id}")
    public String processDeleteAppliance(@PathVariable Long id, Principal principal) {
        String admin = principal != null ? principal.getName() : "ADMIN";
        log.info("Admin '{}' deleting appliance id={}", admin, id);

        applianceService.deleteApplianceById(id);
        log.info("Appliance id={} successfully deleted by admin '{}'", id, admin);

        return "redirect:/admin/appliances";
    }

    private void populateFormData(Model model) {
        List<ManufacturerResponse> manufacturersResponse = manufacturerService.getAllManufacturer();
        model.addAttribute("activePage", "appliances");
        model.addAttribute("manufacturers", manufacturersResponse);
        model.addAttribute("categories", Category.getCategories());
        model.addAttribute("powerType", PowerType.getPowerTypes());
    }
}