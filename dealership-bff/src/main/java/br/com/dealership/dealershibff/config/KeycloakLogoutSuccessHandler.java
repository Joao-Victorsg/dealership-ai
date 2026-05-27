package br.com.dealership.dealershibff.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Ensures browser logout always reaches Keycloak end-session endpoint, even
 * when current Authentication is JwtAuthenticationToken (session-injected bearer).
 */
public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {

    private final String keycloakExternalUrl;
    private final String keycloakRealm;
    private final String postLogoutRedirectUri;
    private final String keycloakClientId;

    public KeycloakLogoutSuccessHandler(
            final String keycloakExternalUrl,
            final String keycloakRealm,
            final String postLogoutRedirectUri,
            final String keycloakClientId) {
        this.keycloakExternalUrl = trimTrailingSlash(keycloakExternalUrl);
        this.keycloakRealm = keycloakRealm;
        this.postLogoutRedirectUri = normalizePostLogoutRedirectUri(postLogoutRedirectUri);
        this.keycloakClientId = keycloakClientId;
    }

    @Override
    public void onLogoutSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException, ServletException {
        final String endSessionEndpoint = keycloakExternalUrl
                + "/realms/" + keycloakRealm + "/protocol/openid-connect/logout";

        final var redirectBuilder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                .queryParam("client_id", keycloakClientId)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);

        final String idTokenHint = extractIdToken(authentication);
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            redirectBuilder.queryParam("id_token_hint", idTokenHint);
        }

        response.sendRedirect(redirectBuilder.build(true).toUriString());
    }

    private String extractIdToken(final Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken)) {
            return null;
        }
        if (!(oauth2AuthenticationToken.getPrincipal() instanceof OidcUser oidcUser)) {
            return null;
        }
        if (oidcUser.getIdToken() == null) {
            return null;
        }
        return oidcUser.getIdToken().getTokenValue();
    }

    private String trimTrailingSlash(final String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizePostLogoutRedirectUri(final String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        final var uri = UriComponentsBuilder.fromUriString(value).build(true).toUri();
        final String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return value.endsWith("/") ? value : value + "/";
        }
        return value;
    }
}
