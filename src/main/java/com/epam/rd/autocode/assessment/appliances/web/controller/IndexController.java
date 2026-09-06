package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
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

        int currentPage = Math.max(page, 0);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        PageRequest pageable = PageRequest.of(currentPage, 8, sort);

        log.debug("Rendering home page: page={}, sortField='{}', sortDir='{}', keyword='{}'",
                currentPage, sortField, sortDir, keyword);

        Page<ApplianceSummaryResponse> appliancePage = applianceService.getAllAppliance(
                keyword, null, null, null, null, null, null, pageable
        );

        List<ManufacturerResponse> manufacturers = manufacturerService.getAllManufacturer();

        model.addAttribute("appliances", appliancePage.getContent());
        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", appliancePage.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);
        model.addAttribute("baseUrl", "/");

        return "index";
    }

    @GetMapping("/about")
    public String showAboutPage(Model model) {
        model.addAttribute("activePage", "about");
        return "about-us";
    }

    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("activePage", "contact");
        return "contact";
    }

    @PostMapping("/contact")
    public String processContactPage() {
        log.info("Contact form submitted");
        return "redirect:/contact";
    }
}