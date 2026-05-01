package br.com.dealership.dealershibff.web;

import br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionTokenInjectionFilterTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private HttpSession session;
    @Mock
    private OAuth2AuthorizedClient refreshedClient;
    @Mock
    private OAuth2AccessToken refreshedAccessToken;
    @Mock
    private OAuth2RefreshToken refreshedRefreshToken;

    private SessionTokenInjectionFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new SessionTokenInjectionFilter(authorizedClientManager);
    }

    @Test
    void shouldPassThroughWhenNoSessionExists() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authorizedClientManager);
    }

    @Test
    void shouldPassThroughWhenNoAccessTokenInSession() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authorizedClientManager);
    }

    @Test
    void shouldInjectBearerHeaderWhenTokenIsValid() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("valid-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY))
                .thenReturn(Instant.now().plusSeconds(3600));

        final var requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertThat(requestCaptor.getValue().getHeader("Authorization")).isEqualTo("Bearer valid-token");
    }

    @Test
    void shouldRefreshAndInjectNewTokenWhenExpired() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        final Instant newExpiry = Instant.now().plusSeconds(3600);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);

        final Authentication auth = mock(Authentication.class);
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        when(refreshedAccessToken.getTokenValue()).thenReturn("new-token");
        when(refreshedAccessToken.getExpiresAt()).thenReturn(newExpiry);
        when(refreshedRefreshToken.getTokenValue()).thenReturn("new-refresh-token");
        when(refreshedClient.getAccessToken()).thenReturn(refreshedAccessToken);
        when(refreshedClient.getRefreshToken()).thenReturn(refreshedRefreshToken);
        when(authorizedClientManager.authorize(any())).thenReturn(refreshedClient);

        final var requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertThat(requestCaptor.getValue().getHeader("Authorization")).isEqualTo("Bearer new-token");
        verify(session).setAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN, "new-token");
        verify(session).setAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY, newExpiry);
        verify(session).setAttribute(OAuth2LoginSuccessHandler.SESSION_REFRESH_TOKEN, "new-refresh-token");
    }

    @Test
    void shouldInvalidateSessionAndPassThroughWhenRefreshFails() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);

        final Authentication auth = mock(Authentication.class);
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        when(authorizedClientManager.authorize(any())).thenThrow(new RuntimeException("Keycloak down"));

        filter.doFilterInternal(request, response, filterChain);

        verify(session).invalidate();
        final var cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("SESSION");
        assertThat(cookieCaptor.getValue().getMaxAge()).isZero();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldInvalidateSessionWhenNullAuthenticationOnRefresh() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);
        SecurityContextHolder.clearContext();

        filter.doFilterInternal(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authorizedClientManager);
    }

    @Test
    void shouldInjectTokenWhenExpiryAttributeIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("valid-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(null);

        final var requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertThat(requestCaptor.getValue().getHeader("Authorization")).isEqualTo("Bearer valid-token");
    }

    @Test
    void shouldInvalidateSessionWhenAuthorizeReturnsNull() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);

        final Authentication auth = mock(Authentication.class);
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRefreshWithoutStoringRefreshTokenWhenNoneReturned() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        final Instant newExpiry = Instant.now().plusSeconds(3600);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);

        final Authentication auth = mock(Authentication.class);
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        when(refreshedAccessToken.getTokenValue()).thenReturn("new-token");
        when(refreshedAccessToken.getExpiresAt()).thenReturn(newExpiry);
        when(refreshedClient.getAccessToken()).thenReturn(refreshedAccessToken);
        when(refreshedClient.getRefreshToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any())).thenReturn(refreshedClient);

        final var requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertThat(requestCaptor.getValue().getHeader("Authorization")).isEqualTo("Bearer new-token");
        verify(session, never()).setAttribute(eq(OAuth2LoginSuccessHandler.SESSION_REFRESH_TOKEN), any());
    }

    @Test
    void shouldPassThroughWhenAccessTokenIsBlank() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authorizedClientManager);
    }

    @Test
    void shouldInvalidateSessionWhenClientHasNullAccessToken() throws Exception {
        final Instant expired = Instant.now().minusSeconds(60);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("old-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY)).thenReturn(expired);

        final Authentication auth = mock(Authentication.class);
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        when(refreshedClient.getAccessToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any())).thenReturn(refreshedClient);

        filter.doFilterInternal(request, response, filterChain);

        verify(session).invalidate();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldStripeClientSuppliedAuthorizationHeader() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN)).thenReturn("session-token");
        when(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY))
                .thenReturn(Instant.now().plusSeconds(3600));

        final var requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertThat(requestCaptor.getValue().getHeader("Authorization")).isEqualTo("Bearer session-token");
        // also verify getHeaders() on the wrapper injects the same token
        assertThat(requestCaptor.getValue().getHeaders("Authorization").nextElement()).isEqualTo("Bearer session-token");
    }
}
