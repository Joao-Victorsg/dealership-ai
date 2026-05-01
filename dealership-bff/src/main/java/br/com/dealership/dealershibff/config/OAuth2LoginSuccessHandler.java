package br.com.dealership.dealershibff.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SESSION_ACCESS_TOKEN = "bff.access_token";
    public static final String SESSION_REFRESH_TOKEN = "bff.refresh_token";
    public static final String SESSION_ID_TOKEN = "bff.id_token";
    public static final String SESSION_TOKEN_EXPIRY = "bff.token_expiry";

    private final OAuth2AuthorizedClientService clientService;
    private final String postLoginRedirectUri;

    public OAuth2LoginSuccessHandler(
            final OAuth2AuthorizedClientService clientService,
            @Value("${app.post-login-redirect-uri}") final String postLoginRedirectUri) {
        this.clientService = clientService;
        this.postLoginRedirectUri = postLoginRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException {

        final var oauthToken = (OAuth2AuthenticationToken) authentication;
        final OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

        if (client == null) {
            response.sendRedirect(postLoginRedirectUri);
            return;
        }

        final HttpSession session = request.getSession();
        final var accessToken = client.getAccessToken();
        session.setAttribute(SESSION_ACCESS_TOKEN, accessToken.getTokenValue());
        session.setAttribute(SESSION_TOKEN_EXPIRY, accessToken.getExpiresAt());

        if (client.getRefreshToken() != null) {
            session.setAttribute(SESSION_REFRESH_TOKEN, client.getRefreshToken().getTokenValue());
        }

        if (oauthToken.getPrincipal() instanceof OidcUser oidcUser && oidcUser.getIdToken() != null) {
            session.setAttribute(SESSION_ID_TOKEN, oidcUser.getIdToken().getTokenValue());
        }

        response.sendRedirect(postLoginRedirectUri);
    }
}
