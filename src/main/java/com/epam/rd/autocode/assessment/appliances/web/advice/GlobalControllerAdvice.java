package com.epam.rd.autocode.assessment.appliances.web.advice;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final ManufacturerService manufacturerService;

    @ModelAttribute("currentUrl")
    public String getCurrentUrl(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @ModelAttribute("brands")
    public List<String> brands() {
        return manufacturerService.getAllManufacturerNames();
    }

    @ModelAttribute("cartItemCount")
    public Integer cartItemCount(Principal principal) {
        if (principal == null) {
            return 0;
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User with email " + principal.getName() + " not found"));

        if (user instanceof Client client) {
            return cartService.getCartItemCount(client.getEmail());
        }

        return 0;
    }
}
