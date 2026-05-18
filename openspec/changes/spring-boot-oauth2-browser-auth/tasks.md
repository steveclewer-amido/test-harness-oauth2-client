## 1. Dependencies & Configuration

- [x] 1.1 Add `spring-boot-starter-oauth2-client` dependency to `pom.xml`
- [x] 1.2 Add `spring-boot-starter-security` dependency to `pom.xml` (if not already present)
- [x] 1.3 Add `spring.security.oauth2.client.registration.<provider>` block to `application.yml` using `${ENV_VAR}` placeholders for `client-id`, `client-secret`, `redirect-uri`, and `scope`
- [x] 1.4 Add `spring.security.oauth2.client.provider.<provider>` block to `application.yml` with `authorization-uri`, `token-uri`, `jwk-set-uri`, and `user-info-uri` placeholders

## 2. Security Configuration

- [x] 2.1 Update `SecurityConfig.java` to enable `.oauth2Login()` on the `HttpSecurity` builder
- [x] 2.2 Configure `authorizeHttpRequests` to permit `/`, `/login*`, `/oauth2/**`, and static resources; require authentication for `/tokens`
- [x] 2.3 Configure logout to invalidate the session and redirect to `/`

## 3. Token Display Controller

- [x] 3.1 Add a `/tokens` GET endpoint to `HomeController` (or a new `TokenController`) that accepts `@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient` and `Authentication`
- [x] 3.2 Populate the model with access token value, ID token value (if present), refresh token value (if present), and principal attributes map
- [x] 3.3 Return the `tokens` view name

## 4. Templates

- [x] 4.1 Create `src/main/resources/templates/tokens.html` Thymeleaf template that renders access token, ID token, refresh token, and principal attributes in a readable layout
- [x] 4.2 Add a visible warning on `tokens.html` that this page is for local development / testing only and must not be deployed publicly
- [x] 4.3 Add a logout form/button to `tokens.html` (POST to `/logout` to satisfy CSRF protection)
- [x] 4.4 Update `index.html` to include a "Login with OAuth2" link pointing to the provider's login initiation URL (e.g., `/oauth2/authorization/<provider>`)

## 5. Verification

- [x] 5.1 Run `mvn spring-boot:run` with required environment variables set and confirm redirect to OAuth2 provider login page
- [x] 5.2 Complete the login flow and confirm `/tokens` page displays a non-empty access token
- [x] 5.3 Confirm that navigating directly to `/tokens` without being logged in redirects to the provider login page
- [x] 5.4 Confirm the logout link invalidates the session and returns the user to the public landing page
