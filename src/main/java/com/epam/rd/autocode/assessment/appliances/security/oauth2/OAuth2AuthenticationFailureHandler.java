package com.epam.rd.autocode.assessment.appliances.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.warn("OAuth2 authentication failed: {}", exception.getMessage());

        String targetUrl = "/login";

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            String errorCode = error.getErrorCode();

            targetUrl = UriComponentsBuilder.fromUriString("/login")
                    .queryParam("oauth2_error", errorCode != null ? errorCode : "true")
                    .build()
                    .toUriString();
        } else {
            targetUrl = "/login?oauth2_error=true";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
