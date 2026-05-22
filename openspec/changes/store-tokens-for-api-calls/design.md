## Context

The test harness performs an OAuth2 Authorization Code flow and displays the resulting tokens at `/home`. Tokens are held by Spring Security's `InMemoryOAuth2AuthorizedClientService` (the default), keyed by registration ID and principal name — they are session-local and not persisted to disk.

Currently there is no way to use those tokens to make outbound HTTP calls without leaving the UI. The goal of this change is to let the tester trigger a small set of pre-configured OIDC provider API calls directly from the browser and see the raw JSON response inline.

**Stack constraints**:
- Spring Boot 2.7.18, traditional Servlet stack (spring-boot-starter-web)
- No Reactor / WebFlux on the classpath (not in `pom.xml`)
- Spring Security 5.7.x — `OAuth2AuthorizedClientManager` is available without additional dependencies

## Goals / Non-Goals

**Goals:**
- Provide a session-scoped service that retrieves (and, if expired, refreshes) the current `OAuth2AuthorizedClient` on demand.
- Add an `/api-calls` page where the user can invoke the OIDC provider's UserInfo endpoint and (optionally) token introspection endpoint, and see the raw JSON response.
- Keep the change self-contained: no new Maven dependencies.

**Non-Goals:**
- Persistent token storage across sessions or server restarts.
- A general-purpose API proxy or request builder.
- Implementing token introspection client-credential authentication (introspection is shown only when `APP_INTROSPECTION_URI` is configured).
- Token refresh initiated by the user from the UI — refresh happens automatically and transparently before each API call.

## Decisions

### 1. Use `DefaultOAuth2AuthorizedClientManager` for token lifecycle management

`OAuth2AuthorizedClientManager` is the recommended Spring Security 5 abstraction. It composes an `OAuth2AuthorizedClientProvider` chain (authorization code + refresh token) so that an expired access token is automatically exchanged for a fresh one via the refresh token before the outbound call — without any custom expiry-check logic.

**Alternative considered**: Read `OAuth2AuthorizedClient` directly from `OAuth2AuthorizedClientService` and manually check `getAccessToken().getExpiresAt()`. Rejected because it duplicates logic already in the framework and silently breaks when the refresh token is absent.

### 2. Use `RestTemplate` with a manual `Authorization: Bearer` header rather than `WebClient`

Since the project is a pure Servlet-stack application with no WebFlux dependency, `RestTemplate` (available from `spring-boot-starter-web`) is the straightforward choice. Adding `spring-boot-starter-webflux` solely for `WebClient` would bring in Netty and Reactor, which is disproportionate for a test harness making two simple GET/POST calls.

The `ApiCallService` will accept a resolved access-token string and a target URI, build a `RequestEntity` with the `Authorization` header, and delegate to a shared `RestTemplate` bean.

**Alternative considered**: `WebClient` — better for reactive pipelines; not needed here and would add ~3 MB of transitive dependencies.

### 3. Derive UserInfo URI from the provider's `ClientRegistration` metadata

Spring Security populates `ClientRegistration.getProviderDetails().getUserInfoEndpoint().getUri()` from the OIDC discovery document when `issuer-uri` is configured. No additional environment variable is needed for UserInfo. Introspection, which is not part of OIDC core discovery, requires an explicit `APP_INTROSPECTION_URI` environment variable; the `/api-calls` page hides the introspection panel when that property is absent.

### 4. `ApiCallsController` — not a separate `TokenStoreService` bean

The proposal named this `TokenStoreService`, but on closer inspection Spring Security already _is_ the token store. The controller simply injects `OAuth2AuthorizedClientManager` and `ClientRegistrationRepository` to obtain both a fresh client and the endpoint URIs. A separate `TokenStoreService` would be an empty wrapper — the logic lives cleanly in `ApiCallsController` and a focused `ApiCallService` helper.

## Risks / Trade-offs

- **Refresh token absent** → `OAuth2AuthorizedClientManager` will return `null` when it cannot refresh an expired token. `ApiCallService` must detect the `null` result and return a user-readable error rather than an NPE. Mitigation: null-check after `manager.authorize()` and surface "session expired — please log in again" in the UI.
- **Introspection requires client credentials** → some IdPs (including ForgeRock AM) protect the introspection endpoint with HTTP Basic using `client_id`/`client_secret`. Since these are already in `application.yml` they will be included, but the IdP may still reject the call if the grant type is not enabled. Mitigation: render the HTTP status code and raw error body in the response panel; do not swallow errors.
- **CORS / TLS** → outbound calls from the server to ForgeRock AM use the same TLS trust store. If the AM certificate is self-signed and not in the JVM truststore the call will fail with an SSL handshake error. Mitigation: document the requirement in `README.md`; no code change needed (same constraint already exists for the OIDC discovery call at startup).
