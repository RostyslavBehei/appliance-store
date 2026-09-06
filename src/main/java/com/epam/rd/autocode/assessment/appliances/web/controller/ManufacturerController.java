package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/brand")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ApplianceService applianceService;

    @GetMapping("/{brandName}")
    public String showApplianceManufacturerPage(
            @PathVariable String brandName,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int currentPage = Math.max(page, 0);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(currentPage, 8, sort);

        log.debug("Rendering brand page for '{}': page={}, sortField='{}', sortDir='{}'",
                brandName, currentPage, sortField, sortDir);

        Page<ApplianceSummaryResponse> response = applianceService.getAllApplianceByManufacturerName(brandName, pageable);

        log.debug("Found {} appliances for brand '{}'", response.getTotalElements(), brandName);

        model.addAttribute("appliances", response.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentBrand", brandName);
        model.addAttribute("baseUrl", "/brand/" + brandName);

        return "index";
    }
}