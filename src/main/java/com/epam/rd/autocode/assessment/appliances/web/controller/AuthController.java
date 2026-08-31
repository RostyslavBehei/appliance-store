package com.epam.rd.autocode.assessment.appliances.web.controller;


import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserLoginRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.UserPasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClientService clientService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", UserLoginRequest.empty());
        return "auth/auth-login-basic";
    }

    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") UserLoginRequest loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return "auth/auth-login-basic";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            return "redirect:/";
        } catch (AuthenticationException ex) {
            return "redirect:/login?error=true";
        }
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("clientRegisterRequest", ClientRegisterRequest.empty());
        return "auth/auth-register-basic";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "auth/auth-forgot-password-page";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage() {
        return "auth/auth-reset-password-page";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email
    ) {
        userService.forgotPassword(email);
        return "redirect:/login";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @ModelAttribute UserPasswordResetRequest request
    ) {

        userService.resetPassword(token, request);
        return "redirect:/login";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("clientRegisterRequest") ClientRegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "auth/auth-register-basic";
        }

        try {
            clientService.createClient(request);
        } catch (Exception ex) {
            return "redirect:/register?error=true";
        }

        return "redirect:/";
    }
}
