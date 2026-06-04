## Context

The test harness already supports two Auth0 login paths:
1. **Direct** — passes `connection=Username-Password-Authentication` (or `OAUTH2_CONNECTION_DIRECT`)
2. **Keycloak-federated** — passes `connection=keycloak-alpha` (or `OAUTH2_CONNECTION_KEYCLOAK`)

Both are handled by `ConnectionAwareRequestResolver`, a custom `OAuth2AuthorizationRequestResolver` that wraps Spring Security's `DefaultOAuth2AuthorizationRequestResolver` and injects additional parameters into the authorize request without embedding them in `authorization-uri` (which Spring Security requires to be a clean URL).

The goal is to add a third path that injects `organization=<org-id>` instead of (or in addition to) a `connection` parameter.

**Stack constraints**: Spring Boot 2.7.18, Spring Security 5.7.x, Java 17, no WebFlux.

## Goals / Non-Goals

**Goals:**
- Inject `organization` parameter for a designated registration via `ConnectionAwareRequestResolver`
- Keep the existing `connection` injection for `auth0` and `auth0-keycloak` registrations entirely unchanged
- Make the org ID configurable via `OAUTH2_ORG_ID` environment variable with no default (omitting the variable simply means the org button exists but sends no `organization` param — acceptable for development)
- Register a new `auth0-org` client with its own redirect URI so Auth0 callback routing is unambiguous

**Non-Goals:**
- Supporting multiple simultaneous organizations (one org per deployment is sufficient for a test harness)
- Dynamic org selection in the UI (the org ID is fixed at startup via env var)
- Combining `connection` and `organization` on the same registration

## Decisions

### Extend `ConnectionAwareRequestResolver` with a second map rather than a new resolver class

The resolver already handles the pattern of "registration ID → extra parameter". Adding a second `organizationsByRegistrationId` map keeps all parameter injection in one place and avoids duplicating the delegate wiring. The `withConnection` method is renamed conceptually (it now also handles organization) but its signature is unchanged — it reads both maps and applies whichever entry exists.

**Alternative considered**: A second resolver that chains the first. Rejected because Spring Security only allows one `authorizationRequestResolver` — chaining would require the second resolver to call the first, re-introduce the same two-map structure inside the chain, and add indirection with no benefit.

### Separate `auth0-org` registration from `auth0`

Reusing the `auth0` registration and injecting `organization` conditionally (e.g., based on a request parameter) would require reading HTTP request state inside the resolver and would conflate two distinct login intents. A separate registration gives Auth0 a distinct redirect URI to match and makes the Spring Security session unambiguous about which flow was used.

**Alternative considered**: A single `auth0` registration with a query parameter to toggle org mode. Rejected because it requires URI-level routing logic in the resolver and mixes concerns.

### `OAUTH2_ORG_ID` with empty default

Using `${OAUTH2_ORG_ID:}` (empty string default) means the application starts without the variable set — the org button appears but sends no `organization` parameter. This is intentional: it lets developers run the app without configuring an org ID when they only need the other two flows.

**Alternative considered**: Require `OAUTH2_ORG_ID` (no default, startup failure if absent). Rejected because it would break existing workflows where the variable is not needed.

## Risks / Trade-offs

- **Empty `OAUTH2_ORG_ID`** → If the variable is not set, the `auth0-org` flow sends no `organization` parameter. Auth0 will treat this as a standard login, which may or may not succeed depending on the tenant's Home Realm Discovery rules. Mitigation: document clearly in README and consider adding a UI hint when the org ID is absent.
- **Auth0 application must be enabled for the organization** → This is an Auth0 dashboard configuration step that cannot be validated at startup. If it is missing, the callback will return an `access_denied` error. Mitigation: documented in README troubleshooting section.
- **Redirect URI must be registered in Auth0** → Same constraint as the existing two URIs. Mitigation: documented in README.

## Migration Plan

1. Add `auth0-org` registration and provider blocks to `application.yml`
2. Add `app.organizations.auth0-org: ${OAUTH2_ORG_ID:}` to `application.yml`
3. Add `auth0OrgId` field (bound to `app.organizations.auth0-org`) to `SecurityConfig`
4. Add `organizationsByRegistrationId` map parameter to `ConnectionAwareRequestResolver`; update `withConnection` to inject `organization` when present
5. Update `securityFilterChain` to pass `Map.of("auth0-org", auth0OrgId)` as the org map
6. Add `redirectUriOrg` field to `HomeController` and pass it to the index model
7. Add "Login with Organization" button and third redirect URI to `index.html`
8. Update README

**Rollback:** Revert the above changes. No schema migrations, no persistent state.
