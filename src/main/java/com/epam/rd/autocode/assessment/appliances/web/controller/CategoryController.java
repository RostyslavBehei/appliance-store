package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ApplianceService applianceService;

    @GetMapping("/{categoryName}")
    public String showApplianceCategoryPage(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Category category;
        try {
            category = Category.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageable = PageRequest.of(page, 8, sort);

        Page<ApplianceSummaryResponse> response = applianceService.getAllApplianceByCategory(category, pageable);

        model.addAttribute("appliances", response.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentCategory", category.name());
        model.addAttribute("baseUrl", "/category/" + category.name());

        return "index";
    }
}
