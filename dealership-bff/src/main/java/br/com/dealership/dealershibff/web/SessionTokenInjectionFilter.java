package br.com.dealership.dealershibff.web;

import br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SessionTokenInjectionFilter extends OncePerRequestFilter {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public SessionTokenInjectionFilter(final OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final HttpSession session = request.getSession(false);
        if (session == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String accessToken = (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN);
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        final Instant expiry = (Instant) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY);
        final String tokenToInject;

        if (expiry != null && Instant.now().isAfter(expiry)) {
            final String refreshed = tryRefresh(request, response, session);
            if (refreshed == null) {
                filterChain.doFilter(request, response);
                return;
            }
            tokenToInject = refreshed;
        } else {
            tokenToInject = accessToken;
        }

        filterChain.doFilter(new BearerTokenRequestWrapper(request, tokenToInject), response);
    }

    private String tryRefresh(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session) {
        try {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                invalidateSession(session, response);
                return null;
            }

            final var authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak")
                    .principal(authentication)
                    .attribute(HttpServletRequest.class.getName(), request)
                    .attribute(HttpServletResponse.class.getName(), response)
                    .build();

            final var client = authorizedClientManager.authorize(authorizeRequest);
            if (client == null || client.getAccessToken() == null) {
                invalidateSession(session, response);
                return null;
            }

            session.setAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN, client.getAccessToken().getTokenValue());
            session.setAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY, client.getAccessToken().getExpiresAt());
            if (client.getRefreshToken() != null) {
                session.setAttribute(OAuth2LoginSuccessHandler.SESSION_REFRESH_TOKEN, client.getRefreshToken().getTokenValue());
            }
            return client.getAccessToken().getTokenValue();

        } catch (Exception e) {
            logger.warn("Token refresh failed [requestId=" + MDC.get("requestId") + "] — invalidating session");
            invalidateSession(session, response);
            return null;
        }
    }

    private void invalidateSession(final HttpSession session, final HttpServletResponse response) {
        try {
            session.invalidate();
        } catch (Exception ignored) {
        }
        final var cookie = new Cookie("SESSION", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private static final class BearerTokenRequestWrapper extends HttpServletRequestWrapper {

        private final String bearerToken;

        BearerTokenRequestWrapper(final HttpServletRequest request, final String bearerToken) {
            super(request);
            this.bearerToken = bearerToken;
        }

        @Override
        public String getHeader(final String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return "Bearer " + bearerToken;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of("Bearer " + bearerToken));
            }
            return super.getHeaders(name);
        }
    }
}
