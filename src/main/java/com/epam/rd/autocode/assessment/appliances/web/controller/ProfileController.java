package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserEmailChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfilePage(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getUserByEmail(principal.getName());

        model.addAttribute("user", profile);

        model.addAttribute("isClient", profile instanceof ClientProfileResponse);
        model.addAttribute("isEmployee", profile instanceof EmployeeProfileResponse);
        return "profile/user-profile-page";
    }

    @GetMapping("/update")
    public String showUpdateProfilePage(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getUserByEmail(principal.getName());
        model.addAttribute("user", profile);

        model.addAttribute("isClient", profile instanceof ClientProfileResponse);
        model.addAttribute("isEmployee", profile instanceof EmployeeProfileResponse);

        return "profile/user-profile-update-page";
    }

    @PostMapping("/change-password")
    public String processChangePassword(
            @Valid @ModelAttribute UserPasswordChangeRequest request,
            Principal principal,
            HttpServletRequest hsr) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            userService.updatePassword(principal.getName(), request);
            hsr.logout();
            return "redirect:/login?passwordChanged=true";
        } catch (IllegalArgumentException e) {
            return "redirect:/profile/update?error=password";
        } catch (ServletException e) {
            throw new RuntimeException("Error while logging out after password change", e);
        }
    }


    @PostMapping("/change-email")
    public String processChangeEmail(
            @Valid @ModelAttribute UserEmailChangeRequest request,
            Principal principal,
            HttpServletRequest hsr) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            userService.updateEmail(principal.getName(), request);
            hsr.logout();
            return "redirect:/login?emailChanged=true";
        } catch (IllegalArgumentException e) {
            return "redirect:/profile/update?error=email";
        } catch (ServletException e) {
            throw new RuntimeException("Error while logging out after email change", e);
        }
    }

    @PostMapping("/update")
    public String processUpdateProfile(
            @Valid @ModelAttribute UserProfileUpdateRequest request,
            Principal principal) {

        userService.updateUser(principal.getName(), request);
        return "redirect:/profile";
    }

    @PostMapping("/deactivate")
    public String processDeactivateProfile(
            Principal principal,
            HttpServletRequest request) {

        userService.deleteUserByEmail(principal.getName());

        try {
            request.logout();
        } catch (ServletException e) {
            throw new RuntimeException("Error while logging out after account deletion", e);
        }

        return "redirect:/login?logout";
    }

}

