## Why

The test harness currently displays tokens after login but has no mechanism to reuse them for outbound API calls against an API that authenticates against the OIDC server (or the OIDC provider itself). As a testing tool, it should be able to exercise protected endpoints (e.g. UserInfo, token introspection) using the tokens it already holds, without requiring the user to copy-paste values manually.

## What Changes

- Introduce a token store service that persists the access token (and refresh token when present) for the current authenticated session.
- Add an `OAuth2AuthorizedClientManager`-backed HTTP client (`WebClient`) that can make authenticated requests to OIDC provider endpoints, automatically refreshing the access token when it has expired.
- Expose a `/api-calls` page (authenticated) where the user can trigger pre-configured test requests (e.g. UserInfo endpoint, token introspection) and see the response inline.
- The token store is scoped to the HTTP session; no tokens are written to disk or a shared store.

## Capabilities

### New Capabilities

- `token-store`: In-memory, session-scoped service that holds `OAuth2AuthorizedClient` credentials and exposes them for use in outbound HTTP requests.
- `oidc-api-calls`: Authenticated page at `/api-calls` that lets the user invoke configurable OIDC provider endpoints (UserInfo, introspection) using the stored tokens and displays the raw JSON response.

### Modified Capabilities

- `token-display`: The existing `/tokens` (home) page gains a link to the new `/api-calls` page.

## Impact

- **New code**: `TokenStoreService`, `ApiCallsController`, `api-calls.html` template.
- **Changed code**: `HomeController` / `home.html` — add navigation link; `SecurityConfig` — register `OAuth2AuthorizedClientManager` bean and `WebClient` bean.
- **Dependencies**: No new libraries required; `spring-boot-starter-webflux` (for `WebClient`) or `spring-boot-starter-web` `RestTemplate` — whichever is already available.
- **Configuration**: Two new optional environment variables — `APP_USERINFO_URI` and `APP_INTROSPECTION_URI` — to override provider endpoint URLs; fall back to values derived from the provider's discovery document.
