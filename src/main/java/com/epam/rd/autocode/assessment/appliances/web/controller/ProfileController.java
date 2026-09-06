package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserEmailChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfilePage(Principal principal, Model model) {
        if (principal == null) {
            log.warn("Unauthorized attempt to access profile page");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.debug("Rendering profile page for user: '{}'", email);

        UserProfileResponse profile = userService.getUserByEmail(email);

        model.addAttribute("user", profile);
        model.addAttribute("isClient", profile instanceof ClientProfileResponse);
        model.addAttribute("isEmployee", profile instanceof EmployeeProfileResponse);

        return "profile/user-profile-page";
    }

    @GetMapping("/update")
    public String showUpdateProfilePage(Principal principal, Model model) {
        if (principal == null) {
            log.warn("Unauthorized attempt to access update profile page");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.debug("Rendering update profile page for user: '{}'", email);

        UserProfileResponse profile = userService.getUserByEmail(email);

        model.addAttribute("user", profile);
        model.addAttribute("isClient", profile instanceof ClientProfileResponse);
        model.addAttribute("isEmployee", profile instanceof EmployeeProfileResponse);

        return "profile/user-profile-update-page";
    }

    @PostMapping("/change-password")
    public String processChangePassword(
            @Valid @ModelAttribute UserPasswordChangeRequest request,
            BindingResult bindingResult,
            Principal principal,
            HttpServletRequest hsr) {

        if (principal == null) {
            log.warn("Unauthorized submission to change password");
            return "redirect:/login";
        }

        String email = principal.getName();

        if (bindingResult.hasErrors()) {
            log.warn("Password change form validation failed for user '{}'", email);
            return "redirect:/profile/update?error=passwordValidation";
        }

        try {
            log.info("Initiating password change for user: '{}'", email);
            userService.updatePassword(email, request);
            log.info("Password successfully changed for user '{}'. Logging out", email);

            hsr.logout();
            return "redirect:/login?passwordChanged=true";
        } catch (IllegalArgumentException e) {
            log.warn("Password change rejected for user '{}': {}", email, e.getMessage());
            return "redirect:/profile/update?error=password";
        } catch (ServletException e) {
            log.error("Error during session logout after password change for '{}': {}", email, e.getMessage(), e);
            throw new RuntimeException("Error while logging out after password change", e);
        }
    }

    @PostMapping("/change-email")
    public String processChangeEmail(
            @Valid @ModelAttribute UserEmailChangeRequest request,
            BindingResult bindingResult,
            Principal principal,
            HttpServletRequest hsr) {

        if (principal == null) {
            log.warn("Unauthorized submission to change email");
            return "redirect:/login";
        }

        String email = principal.getName();

        if (bindingResult.hasErrors()) {
            log.warn("Email change form validation failed for user '{}'", email);
            return "redirect:/profile/update?error=emailValidation";
        }

        try {
            log.info("Initiating email change for user '{}' to '{}'", email, request.newEmail());
            userService.updateEmail(email, request);
            log.info("Email successfully changed for user '{}'. Logging out", email);

            hsr.logout();
            return "redirect:/login?emailChanged=true";
        } catch (IllegalArgumentException e) {
            log.warn("Email change rejected for user '{}': {}", email, e.getMessage());
            return "redirect:/profile/update?error=email";
        } catch (ServletException e) {
            log.error("Error during session logout after email change for '{}': {}", email, e.getMessage(), e);
            throw new RuntimeException("Error while logging out after email change", e);
        }
    }

    @PostMapping("/update")
    public String processUpdateProfile(
            @Valid @ModelAttribute UserProfileUpdateRequest request,
            BindingResult bindingResult,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthorized submission to update profile");
            return "redirect:/login";
        }

        String email = principal.getName();

        if (bindingResult.hasErrors()) {
            log.warn("Profile update validation failed for user '{}': {} error(s)", email, bindingResult.getErrorCount());
            return "redirect:/profile/update?error=profileValidation";
        }

        log.info("Updating profile attributes for user: '{}'", email);
        userService.updateUser(email, request);
        log.info("Profile successfully updated for user: '{}'", email);

        return "redirect:/profile";
    }

    @PostMapping("/deactivate")
    public String processDeactivateProfile(
            Principal principal,
            HttpServletRequest request) {

        if (principal == null) {
            log.warn("Unauthorized attempt to deactivate account");
            return "redirect:/login";
        }

        String email = principal.getName();
        log.info("Deactivating and deleting account for user: '{}'", email);

        userService.deleteUserByEmail(email);

        try {
            request.logout();
            log.info("User '{}' account deactivated and logged out successfully", email);
        } catch (ServletException e) {
            log.error("Error while logging out after account deletion for '{}': {}", email, e.getMessage(), e);
            throw new RuntimeException("Error while logging out after account deletion", e);
        }

        return "redirect:/login?logout";
    }
}