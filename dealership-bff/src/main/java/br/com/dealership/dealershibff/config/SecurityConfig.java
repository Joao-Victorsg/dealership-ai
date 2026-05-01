package br.com.dealership.dealershibff.config;

import br.com.dealership.dealershibff.web.SessionTokenInjectionFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            final SessionTokenInjectionFilter sessionTokenInjectionFilter,
            final ClientRegistrationRepository clientRegistrationRepository){
        http
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/actuator/health/**"
                    ).permitAll()
                    .anyRequest().authenticated())
            .oauth2Login(oauth2 ->
                    oauth2.successHandler(oauth2LoginSuccessHandler))
            .oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwt ->
                            jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .logout(logout -> logout
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                    .deleteCookies("SESSION")
                    .invalidateHttpSession(true))
            .addFilterBefore(sessionTokenInjectionFilter, BearerTokenAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                    .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable));
        return http.build();
    }

    @Bean
    public OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(
            final ClientRegistrationRepository clientRegistrationRepository) {
        final var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
        return handler;
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            final ClientRegistrationRepository clientRegistrationRepository,
            final OAuth2AuthorizedClientRepository authorizedClientRepository) {
        final var manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build());
        return manager;
    }

    @Bean
    public SessionTokenInjectionFilter sessionTokenInjectionFilter(
            final OAuth2AuthorizedClientManager authorizedClientManager) {
        return new SessionTokenInjectionFilter(authorizedClientManager);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
        return converter;
    }

    static final class RolesClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(final Jwt jwt) {
            final List<GrantedAuthority> authorities = new ArrayList<>();

            // Try flat 'roles' claim first
            final List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }

            // Try nested 'realm_access.roles'
            final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                final Object realmRoles = realmAccess.get("roles");
                if (realmRoles instanceof List<?> list) {
                    list.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                }
            }

            return authorities;
        }
    }
}
