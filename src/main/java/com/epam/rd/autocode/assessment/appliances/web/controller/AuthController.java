package com.epam.rd.autocode.assessment.appliances.web.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserLoginRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClientService clientService;
    private final AuthenticationManager authenticationManager;
    private final JwtCore jwtCore;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", UserLoginRequest.empty());
        return "auth/auth-login-basic";
    }

    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") UserLoginRequest loginRequest,
            BindingResult bindingResult,
            HttpServletResponse httpServletResponse) {

        if (bindingResult.hasErrors()) {
            log.warn("Login validation failed for input: {}", loginRequest.email());
            return "auth/auth-login-basic";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtCore.generateToken(userDetails);

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .sameSite("Strict")
                    .build();

            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            log.info("User '{}' successfully authenticated via credentials", loginRequest.email());
            return "redirect:/";

        } catch (DisabledException e) {
            log.warn("Login attempt for disabled account: '{}'", loginRequest.email());
            return "redirect:/login?disabled=true";
        } catch (LockedException e) {
            log.warn("Login attempt for locked account: '{}'", loginRequest.email());
            return "redirect:/login?locked=true";
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt (bad credentials) for: '{}'", loginRequest.email());
            return "redirect:/login?error=true";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new ClientRegisterRequest("", "", "", "", null, ""));
        }
        return "auth/auth-register-basic";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("registerRequest") ClientRegisterRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            log.warn("Client registration form validation failed for: '{}'", request.email());
            return "auth/auth-register-basic";
        }

        try {
            clientService.createClient(request);
            log.info("Client registration initiated for: '{}'", request.email());
            return "redirect:/login";
        } catch (AlreadyExistsException ex) {
            log.warn("Registration conflict for email '{}': {}", request.email(), ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/auth-register-basic";
        } catch (Exception ex) {
            log.error("Unexpected error during registration for '{}': {}", request.email(), ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Registration failed: " + ex.getMessage());
            return "auth/auth-register-basic";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "auth/auth-forgot-password-page";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        log.info("Forgot password requested for email: '{}'", email);
        try {
            userService.forgotPassword(email);
            redirectAttributes.addFlashAttribute("successMessage", true);
            return "redirect:/login";
        } catch (NotFoundException ex) {
            log.warn("Forgot password requested for non-existing email: '{}'", email);
            redirectAttributes.addFlashAttribute("errorMessage", true);
            redirectAttributes.addFlashAttribute("emailValue", email);
            return "redirect:/forgot-password";
        } catch (Exception ex) {
            log.error("Error processing forgot-password for '{}': {}", email, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("generalError", ex.getMessage());
            redirectAttributes.addFlashAttribute("emailValue", email);
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam(value = "token", required = false) String token,
            Model model
    ) {
        if (token == null || token.isBlank()) {
            log.warn("Access attempt to reset password page without token");
            return "redirect:/login";
        }
        model.addAttribute("token", token);
        return "auth/auth-reset-password-page";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @ModelAttribute UserPasswordResetRequest request) {

        log.info("Submitting password reset via token (prefix: '{}')", maskToken(token));
        userService.resetPassword(token, request);
        log.info("Password successfully reset via token");
        return "redirect:/login";
    }

    @GetMapping("/verify")
    public String processVerifyAccount(@RequestParam String token) {
        log.info("Verifying account via token (prefix: '{}')", maskToken(token));
        userService.verifyAccount(token);
        log.info("Account verified successfully via token");
        return "redirect:/login?verified=true";
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 8) + "-****";
    }
}