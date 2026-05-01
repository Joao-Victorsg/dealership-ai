# dealership-bff Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-07-15

## Active Technologies

- Java 25 + Spring Boot 4.0.6, Spring Cloud BOM 2025.1.0, Spring Cloud OpenFeign 5.0.x, resilience4j-spring-boot4 (latest stable), springdoc-openapi-starter-webmvc-ui 3.0.2, Spring Security OAuth2 Resource Server, Spring Data Redis, Spring Boot Actuator, Jackson (via Boot), Lombok (001-bff-orchestration)
- Spring Boot OAuth2 Client (PKCE / Authorization Code), Spring Session Data Redis (distributed HttpSession), OidcClientInitiatedLogoutSuccessHandler, DefaultOAuth2AuthorizedClientManager (transparent token refresh) (002-pkce-auth-migration)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 25

## Code Style

Java 25: Follow standard conventions

## Recent Changes

- 001-bff-orchestration: Added Java 25 + Spring Boot 4.0.6, Spring Cloud BOM 2025.1.0, Spring Cloud OpenFeign 5.0.x, resilience4j-spring-boot4 (latest stable), springdoc-openapi-starter-webmvc-ui 3.0.2, Spring Security OAuth2 Resource Server, Spring Data Redis, Spring Boot Actuator, Jackson (via Boot), Lombok
- 002-pkce-auth-migration: Added spring-boot-starter-oauth2-client + spring-session-data-redis. Migrated auth from ROPC to PKCE Authorization Code flow. New: SessionTokenInjectionFilter, OAuth2LoginSuccessHandler. Removed: TokenRefreshFilter, LoginRequest, TokenResponse, POST /login, POST /refresh. Session stored in Redis; frontend receives only SESSION HttpOnly cookie.

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
