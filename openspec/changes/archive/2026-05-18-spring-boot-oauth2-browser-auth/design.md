## Context

This is a standalone Spring Boot test harness application whose purpose is to exercise and verify OAuth2 provider configurations. Developers need to confirm that an OAuth2 provider (e.g., ForgeRock, Keycloak, Okta) is correctly set up by running the full Authorization Code flow through a browser and inspecting the resulting tokens. The application already has a basic Spring MVC + Thymeleaf scaffold and a minimal `SecurityConfig`.

## Goals / Non-Goals

**Goals:**
- Enable browser-initiated OAuth2 Authorization Code flow against a configurable external provider
- Display the access token, ID token, refresh token, and principal attributes after successful login
- Keep provider credentials (client ID, secret) externalised via environment variables so the app can target different providers without code changes
- Work out-of-the-box with `mvn spring-boot:run` for local developer use

**Non-Goals:**
- Production-grade security hardening (HTTPS enforcement, CSRF hardening beyond defaults, rate limiting)
- Supporting flows other than Authorization Code (Implicit, Client Credentials, Device Code)
- Persisting tokens to a database or external store
- Multi-provider login picker UI

## Decisions

### Use `spring-boot-starter-oauth2-client` rather than manual token handling
Spring Security's OAuth2 Client support handles the PKCE/state/nonce lifecycle, token exchange, and refresh automatically. Rolling this manually would add complexity with no benefit for a test harness.

**Alternatives considered:**
- *Nimbus SDK directly*: More control but significant boilerplate; not warranted here.
- *Spring Authorization Server*: Wrong direction — we are the client, not the server.

### Use `InMemoryOAuth2AuthorizedClientService` (default)
For a single-developer test harness, in-memory storage is sufficient. There is no need for JDBC or Redis-backed storage.

**Alternatives considered:**
- *JdbcOAuth2AuthorizedClientService*: Adds a database dependency for no gain in this context.

### Expose tokens via a dedicated `/tokens` endpoint
Separating the landing page (`/`) from the token display (`/tokens`) keeps concerns distinct and makes it easy to share or bookmark the token view URL.

**Alternatives considered:**
- *Inline on the home page*: Mixes navigation/welcome UI with raw token data; harder to extend.

### Externalise `client-id`, `client-secret`, and provider URIs via environment variables
Sensitive credentials must not be committed to the repository. Spring Boot's `${ENV_VAR}` interpolation in `application.yml` achieves this with zero extra code.

## Risks / Trade-offs

- **Tokens displayed in browser** → Acceptable for a local test harness; add a visible warning in the UI that this is not for production use.
- **No HTTPS in default config** → Access tokens are sent in plain HTTP on localhost. Acceptable for local use only; document that TLS must be enabled before any non-local deployment.
- **In-memory session storage** → Restarting the server invalidates sessions. Expected behaviour for a test harness.
- **Single provider at a time** → `application.yml` supports one registration by default. Developers who need to test multiple providers simultaneously must run separate instances. This is a deliberate simplicity trade-off.

## Migration Plan

1. Add `spring-boot-starter-oauth2-client` dependency to `pom.xml`
2. Update `application.yml` with `spring.security.oauth2.client.registration` and `provider` blocks using `${ENV_VAR}` placeholders
3. Update `SecurityConfig` to enable `.oauth2Login()` and protect `/tokens` (permit `/`, `/login*`, static resources)
4. Add (or update) `HomeController` with a `/tokens` endpoint that injects `@RegisteredOAuth2AuthorizedClient` and `Authentication`
5. Add `tokens.html` Thymeleaf template to render token values and principal attributes
6. Update `index.html` / `home.html` with a login link

**Rollback:** Revert the above changes. No schema migrations, no persistent state to clean up.

## Open Questions

- Which specific OAuth2 provider should be used as the documented example in `README.md` / `application.yml` (ForgeRock AM, Keycloak, generic OIDC)?  
  *Assumption*: Document ForgeRock AM as primary example (matching the existing package namespace) but keep configuration generic so any OIDC-compliant provider works.
