package com.epam.rd.autocode.assessment.appliances.web.controller.admin;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/manufacturers")
@RequiredArgsConstructor
public class AdminManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public String showManufacturerPage(
            @RequestParam(required = false) String keyword,
            Model model) {
        List<ManufacturerResponse> manufacturers = manufacturerService.getAllManufacturer(keyword);

        model.addAttribute("manufacturersList", manufacturers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "manufacturers");

        return "admin/manufacturer/admin-manufacturers-page";
    }

    @GetMapping("/new")
    public String showNewManufacturerPage(Model model) {
        model.addAttribute("manufacturer", ManufacturerResponse.empty());
        model.addAttribute("activePage", "manufacturers");
        return "admin/manufacturer/admin-manufacturers-form-page";
    }

    @GetMapping("/edit/{id}")
    public String showEditManufacturerPage(
            @PathVariable Long id,
            Model model) {

        ManufacturerResponse response = manufacturerService.getManufacturerById(id);
        model.addAttribute("manufacturer", response);
        model.addAttribute("activePage", "manufacturers");
        return "admin/manufacturer/admin-manufacturers-form-page";
    }

    @PostMapping("/save")
    public String processEditManufacturer(
            @ModelAttribute ManufacturerUpdateRequest request
    ) {

        if (request.id() != null) {
            manufacturerService.updateManufacturer(request);
        } else {
            ManufacturerCreateRequest manufacturerCreateRequest = new ManufacturerCreateRequest(request.name());
            manufacturerService.createManufacturer(manufacturerCreateRequest);
        }

        return "redirect:/admin/manufacturers";
    }

    @PostMapping("/delete/{id}")
    public String processDeleteManufacturer(
            @PathVariable Long id
    ) {

        manufacturerService.deleteManufacturerById(id);

        return "redirect:/admin/manufacturers";
    }
}
