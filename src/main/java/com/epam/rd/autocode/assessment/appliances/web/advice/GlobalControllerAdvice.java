package com.epam.rd.autocode.assessment.appliances.web.advice;

import com.epam.rd.autocode.assessment.appliances.model.CartItem;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.CartService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final CartRepository cartRepository;

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
    public Integer cartItemCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return 0;
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .filter(user -> user instanceof Client)
                .flatMap(user -> cartRepository.findByClient((Client) user))
                .map(cart -> {
                    return cart.getItems().stream()
                            .mapToInt(CartItem::getQuantity)
                            .sum();
                })
                .orElse(0);
    }
}
