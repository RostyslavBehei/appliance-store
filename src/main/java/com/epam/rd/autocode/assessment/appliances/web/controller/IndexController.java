package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping("/")
    public String showHomePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageable = PageRequest.of(page, 8, sort);

        Page<ApplianceSummaryResponse> appliancePage = applianceService.getAllAppliance(
                keyword, null, null, null, null, null, null, pageable
        );

        List<ManufacturerResponse> manufacturerPage = manufacturerService.getAllManufacturer();

        model.addAttribute("appliances", appliancePage.getContent());
        model.addAttribute("manufacturers", manufacturerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appliancePage.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);
        model.addAttribute("baseUrl", "/");

        return "index";
    }


}
