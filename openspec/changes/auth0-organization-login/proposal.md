## Why

Auth0 supports multi-tenant applications via its Organizations feature. When a client sends `organization=<org-id>` on the authorize request, Auth0 scopes the login experience to that organization — enforcing membership, applying org-level branding, and including `org_id` / `org_name` in the resulting tokens.

The test harness currently has no way to exercise this flow. Developers testing Auth0 B2B setups need to verify that the `organization` parameter is correctly accepted, that the resulting tokens contain organization claims, and that the app handles the callback correctly. Without this, verifying Auth0 Organizations configuration requires manually constructing authorize URLs, which bypasses Spring Security's state/nonce handling and always fails at the callback.

## What Changes

- Add a third OAuth2 client registration (`auth0-org`) with its own redirect URI (`/login/oauth2/code/auth0-org`)
- Extend `ConnectionAwareRequestResolver` to also inject an `organization` parameter for designated registrations, driven by a new `OAUTH2_ORG_ID` environment variable
- Add a "Login with Organization" button to the index page
- Add `app.organizations` config block to `application.yml` mirroring the existing `app.connections` pattern

## Capabilities

### New Capabilities
- `org-login`: A new login path that passes `organization=<OAUTH2_ORG_ID>` to Auth0's authorize endpoint, scoping the session to a specific Auth0 organization

### Modified Capabilities
- `oauth2-login` (from `2026-05-18-spring-boot-oauth2-browser-auth`): Index page gains a third login button; `ConnectionAwareRequestResolver` gains organization injection alongside the existing connection injection

## Impact

- **Configuration**: `application.yml` gains `auth0-org` registration, `auth0-org` provider, and `app.organizations.auth0-org` property; `OAUTH2_ORG_ID` env var controls the org ID at runtime
- **Security**: `SecurityConfig.java` — `ConnectionAwareRequestResolver` constructor gains a second map parameter; existing connection behaviour is unchanged
- **Controllers**: `HomeController.java` gains a `redirectUriOrg` field (display only)
- **Templates**: `index.html` gains a third login button and shows the third redirect URI
- **Auth0**: The new redirect URI must be registered as an Allowed Callback URL; the Auth0 application must be enabled for the target organization in the Auth0 dashboard
