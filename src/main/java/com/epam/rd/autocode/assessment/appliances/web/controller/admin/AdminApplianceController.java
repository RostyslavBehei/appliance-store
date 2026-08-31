package com.epam.rd.autocode.assessment.appliances.web.controller.admin;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Sort sorting = sort.equals("oldest") ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<ApplianceSummaryResponse> appliancesResponse = applianceService.getAllAppliance(keyword, null, null, null, null, null, null, pageable);

        model.addAttribute("keyword", keyword);
        model.addAttribute("appliances", appliancesResponse);

        return "admin/appliance/admin-appliances-page";
    }

    @GetMapping("/{id}")
    public String showApplianceDetailPage(
            @PathVariable Long id,
            Model model
    ) {
        ApplianceResponse applianceResponse = applianceService.getApplianceById(id);

        model.addAttribute("appliance", applianceResponse);

        return "admin/appliance/admin-appliances-details-page";
    }

    @GetMapping("/edit/{id}")
    public String showEditAppliancePage(@PathVariable Long id, Model model) {
        ApplianceResponse applianceResponse = applianceService.getApplianceById(id);
        List<ManufacturerResponse> manufacturersResponse = manufacturerService.getAllManufacturer();

        model.addAttribute("appliance", applianceResponse);
        model.addAttribute("manufacturers", manufacturersResponse);
        model.addAttribute("categories", Category.getCategories());
        model.addAttribute("powerType", PowerType.getPowerTypes());

        return "admin/appliance/admin-appliances-form-page";
    }

    @GetMapping("/new")
    public String showNewAppliancePage(Model model) {
        List<ManufacturerResponse> manufacturersResponse = manufacturerService.getAllManufacturer();

        model.addAttribute("appliance", ApplianceResponse.empty());
        model.addAttribute("manufacturers", manufacturersResponse);
        model.addAttribute("categories", Category.getCategories());
        model.addAttribute("powerType", PowerType.getPowerTypes());

        return "admin/appliance/admin-appliances-form-page";
    }

    @PostMapping("/save")
    public String processSaveAppliance(
            @ModelAttribute ApplianceUpdateRequest request
    ) {
        applianceService.updateAppliance(request);

        return "redirect:/admin/appliances";
    }

    @PostMapping("/delete/{id}")
    public String processDeleteAppliance(@PathVariable Long id) {
        applianceService.deleteApplianceById(id);
        return "redirect:/admin/appliances";
    }
}
