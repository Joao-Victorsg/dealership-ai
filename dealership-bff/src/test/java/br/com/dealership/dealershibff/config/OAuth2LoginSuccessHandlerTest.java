package br.com.dealership.dealershibff.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

import static br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private OAuth2AuthorizedClientService clientService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;
    @Mock
    private OAuth2AccessToken accessToken;
    @Mock
    private OAuth2RefreshToken refreshToken;
    @Mock
    private OAuth2User oauthUser;

    private static final String POST_LOGIN_URI = "http://localhost:3000";

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(clientService, POST_LOGIN_URI);
    }

    @Test
    void shouldStoreAccessTokenAndRefreshTokenInSessionAndRedirect() throws IOException {
        final Instant expiresAt = Instant.now().plusSeconds(3600);
        when(request.getSession()).thenReturn(session);
        when(accessToken.getTokenValue()).thenReturn("access-token-value");
        when(accessToken.getExpiresAt()).thenReturn(expiresAt);
        when(refreshToken.getTokenValue()).thenReturn("refresh-token-value");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClient.getRefreshToken()).thenReturn(refreshToken);
        when(oauthUser.getName()).thenReturn("sub-123");
        when(clientService.loadAuthorizedClient("keycloak", "sub-123")).thenReturn(authorizedClient);

        final var auth = new OAuth2AuthenticationToken(oauthUser, Collections.emptyList(), "keycloak");
        handler.onAuthenticationSuccess(request, response, auth);

        verify(session).setAttribute(SESSION_ACCESS_TOKEN, "access-token-value");
        verify(session).setAttribute(SESSION_TOKEN_EXPIRY, expiresAt);
        verify(session).setAttribute(SESSION_REFRESH_TOKEN, "refresh-token-value");
        verify(response).sendRedirect(POST_LOGIN_URI);
    }

    @Test
    void shouldStoreIdTokenWhenPrincipalIsOidcUser() throws IOException {
        final Instant expiresAt = Instant.now().plusSeconds(3600);
        when(request.getSession()).thenReturn(session);
        when(accessToken.getTokenValue()).thenReturn("access-token");
        when(accessToken.getExpiresAt()).thenReturn(expiresAt);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClient.getRefreshToken()).thenReturn(null);

        final var idToken = OidcIdToken.withTokenValue("id-token-value")
                .subject("sub-123")
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        final var oidcUser = new DefaultOidcUser(Collections.emptyList(), idToken);
        final var auth = new OAuth2AuthenticationToken(oidcUser, Collections.emptyList(), "keycloak");
        when(clientService.loadAuthorizedClient("keycloak", oidcUser.getName())).thenReturn(authorizedClient);

        handler.onAuthenticationSuccess(request, response, auth);

        verify(session).setAttribute(eq(SESSION_ID_TOKEN), eq("id-token-value"));
    }

    @Test
    void shouldSkipRefreshTokenAttributeWhenRefreshTokenIsNull() throws IOException {
        when(request.getSession()).thenReturn(session);
        when(accessToken.getTokenValue()).thenReturn("access-token");
        when(accessToken.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClient.getRefreshToken()).thenReturn(null);
        when(oauthUser.getName()).thenReturn("sub-123");
        when(clientService.loadAuthorizedClient("keycloak", "sub-123")).thenReturn(authorizedClient);

        final var auth = new OAuth2AuthenticationToken(oauthUser, Collections.emptyList(), "keycloak");
        handler.onAuthenticationSuccess(request, response, auth);

        verify(session, never()).setAttribute(eq(SESSION_REFRESH_TOKEN), any());
    }

    @Test
    void shouldRedirectImmediatelyWhenAuthorizedClientIsNull() throws IOException {
        when(oauthUser.getName()).thenReturn("sub-123");
        when(clientService.loadAuthorizedClient("keycloak", "sub-123")).thenReturn(null);

        final var auth = new OAuth2AuthenticationToken(oauthUser, Collections.emptyList(), "keycloak");
        handler.onAuthenticationSuccess(request, response, auth);

        verify(session, never()).setAttribute(any(), any());
        verify(response).sendRedirect(POST_LOGIN_URI);
    }
}
