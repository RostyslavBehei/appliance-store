package com.epam.rd.autocode.assessment.appliances.web.controller.adminPanel;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
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
@RequestMapping("/admin/manufacturers")
@RequiredArgsConstructor
public class AdminManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public String showManufacturerPage(@RequestParam(required = false) String keyword, Model model) {
        log.debug("Rendering admin manufacturers list: keyword='{}'", keyword);

        List<ManufacturerResponse> manufacturers = manufacturerService.getAllManufacturer(keyword);
        model.addAttribute("manufacturersList", manufacturers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "manufacturers");

        return "admin/manufacturer/admin-manufacturers-page";
    }

    @GetMapping("/new")
    public String showNewManufacturerPage(Model model) {
        log.debug("Opening new manufacturer form");

        model.addAttribute("manufacturer", ManufacturerResponse.empty());
        model.addAttribute("activePage", "manufacturers");
        return "admin/manufacturer/admin-manufacturers-form-page";
    }

    @GetMapping("/edit/{id}")
    public String showEditManufacturerPage(@PathVariable Long id, Model model) {
        log.debug("Opening edit form for manufacturer id: {}", id);

        ManufacturerResponse response = manufacturerService.getManufacturerById(id);
        model.addAttribute("manufacturer", response);
        model.addAttribute("activePage", "manufacturers");
        return "admin/manufacturer/admin-manufacturers-form-page";
    }

    @PostMapping("/save")
    public String processEditManufacturer(
            @Valid @ModelAttribute("manufacturer") ManufacturerUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        String admin = principal != null ? principal.getName() : "ADMIN";

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed for manufacturer save: {} error(s)", bindingResult.getErrorCount());
            model.addAttribute("activePage", "manufacturers");
            return "admin/manufacturer/admin-manufacturers-form-page";
        }

        try {
            if (request.id() != null) {
                log.info("Admin '{}' updating manufacturer id={}, new name='{}'", admin, request.id(), request.name());
                manufacturerService.updateManufacturer(request);
            } else {
                log.info("Admin '{}' creating manufacturer with name='{}'", admin, request.name());
                manufacturerService.createManufacturer(new ManufacturerCreateRequest(request.name()));
            }
            return "redirect:/admin/manufacturers";
        } catch (AlreadyExistsException ex) {
            log.warn("Failed to save manufacturer: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("activePage", "manufacturers");
            return "admin/manufacturer/admin-manufacturers-form-page";
        }
    }

    @PostMapping("/delete/{id}")
    public String processDeleteManufacturer(@PathVariable Long id, Principal principal) {
        String admin = principal != null ? principal.getName() : "ADMIN";
        log.info("Admin '{}' deleting manufacturer id: {}", admin, id);

        manufacturerService.deleteManufacturerById(id);
        log.info("Manufacturer id: {} deleted successfully by '{}'", id, admin);

        return "redirect:/admin/manufacturers";
    }
}