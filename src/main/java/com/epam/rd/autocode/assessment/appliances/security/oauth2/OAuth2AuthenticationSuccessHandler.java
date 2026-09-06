package com.epam.rd.autocode.assessment.appliances.security.oauth2;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtCore;
import com.epam.rd.autocode.assessment.appliances.service.impl.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtCore jwtCore;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String username = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (firstName == null || firstName.isBlank()) {
            firstName = oAuth2User.getAttribute("name");
            if (firstName == null) firstName = "User";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "Google";
        }

        String finalFirstName = firstName;
        String finalLastName = lastName;

        userRepository.findByEmail(username).orElseGet(() -> {

            String uniqueClientCart;

            do {
                uniqueClientCart = generateClientCard();
            } while (clientRepository.existsByCart(uniqueClientCart));

            Client client = Client.builder()
                    .firstName(finalFirstName)
                    .lastName(finalLastName)
                    .middleName("GOOGLE")
                    .email(username)
                    .password("GOOGLE-" + UUID.randomUUID())
                    .role(Role.ROLE_CLIENT)
                    .enabled(true)
                    .birthday(LocalDate.of(2000, 1, 1))
                    .cart(uniqueClientCart)
                    .build();
            return userRepository.save(client);
        });

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        String token = jwtCore.generateToken(userDetails);

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ResponseCookie clearSessionCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearSessionCookie.toString());

        getRedirectStrategy().sendRedirect(request, response, "/");
    }

    private String generateClientCard() {
        int rawInt = UUID.randomUUID().hashCode();

        String padded = String.format("%08d", Math.abs(rawInt % 100000000));

        return padded.substring(0, 4) + "-" + padded.substring(4, 8);
    }
}
