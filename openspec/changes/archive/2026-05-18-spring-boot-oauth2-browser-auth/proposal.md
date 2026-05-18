## Why

This project needs a working standalone Spring Boot application that demonstrates the complete OAuth2 Authorization Code flow via a browser — authenticating against an OAuth2/OIDC provider and displaying the resulting tokens (access token, ID token, refresh token) back to the user. This serves as a developer test harness for verifying OAuth2 provider configurations.

## What Changes

- Add Spring Security OAuth2 Client dependency to enable browser-based OAuth2 login
- Configure `application.yml` with OAuth2 provider registration (client ID, secret, authorization/token endpoints, scopes)
- Implement a secured home page that is only accessible after successful authentication
- Implement a token display page that shows the access token, ID token, and refresh token after login
- Configure Spring Security to require authentication and handle the OAuth2 callback redirect
- Add a login landing page that initiates the OAuth2 authorization code flow

## Capabilities

### New Capabilities
- `oauth2-login`: Browser-initiated OAuth2 Authorization Code flow — redirects to the provider, handles the callback, and establishes an authenticated session
- `token-display`: Post-authentication page that renders the access token, ID token, refresh token, and associated claims/attributes from the OAuth2 authorized client and authenticated principal

### Modified Capabilities
<!-- No existing specs to modify — this is a new project. -->

## Impact

- **Dependencies**: Add `spring-boot-starter-oauth2-client` and `spring-boot-starter-security` to `pom.xml`
- **Configuration**: `application.yml` gains `spring.security.oauth2.client` registration and provider settings; sensitive values (client-secret) should be externalisable via environment variables
- **Security**: `SecurityConfig.java` updated to enable OAuth2 login and protect routes
- **Controllers**: `HomeController.java` updated (or a new `TokenController.java` added) to expose the token display page, injecting `OAuth2AuthorizedClient` and `Authentication`
- **Templates**: `home.html` and/or a new `tokens.html` Thymeleaf template to render token details
