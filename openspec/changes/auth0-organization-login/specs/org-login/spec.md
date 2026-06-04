## ADDED Requirements

### Requirement: Application supports an organization-scoped login path via Auth0 Organizations
The application SHALL provide a login path that passes the `organization` parameter to Auth0's authorize endpoint, scoping the resulting session to a specific Auth0 organization. The org ID SHALL be configurable via the `OAUTH2_ORG_ID` environment variable without code changes.

#### Scenario: User initiates organization login
- **WHEN** an unauthenticated user clicks "Login with Organization"
- **THEN** the application SHALL redirect to Auth0's authorize endpoint with `organization=<OAUTH2_ORG_ID>` included as a query parameter alongside the standard `state`, `nonce`, `scope`, `redirect_uri`, and `response_type` parameters

#### Scenario: Auth0 redirects back after successful organization login
- **WHEN** Auth0 redirects to `https://stevedev-local:8443/login/oauth2/code/auth0-org` with a valid `code` and matching `state`
- **THEN** the application SHALL exchange the code for tokens and establish an authenticated session, and the resulting ID token SHALL contain `org_id` and/or `org_name` claims

#### Scenario: Callback with invalid state parameter
- **WHEN** the callback URI receives a `state` that does not match the stored value
- **THEN** the application SHALL reject the callback and redirect to `/?error`

#### Scenario: `OAUTH2_ORG_ID` is not set
- **WHEN** the application starts without `OAUTH2_ORG_ID`
- **THEN** the application SHALL start successfully; the "Login with Organization" button SHALL appear on the index page; clicking it SHALL initiate a standard Auth0 login with no `organization` parameter

### Requirement: Organization login uses a distinct redirect URI
The `auth0-org` registration SHALL use a redirect URI of `https://stevedev-local:8443/login/oauth2/code/auth0-org`, separate from the `auth0` and `auth0-keycloak` registrations, so that Auth0 can route callbacks unambiguously.

#### Scenario: Redirect URI is shown on the index page
- **WHEN** an unauthenticated user loads the index page
- **THEN** the page SHALL display all three redirect URIs including `https://stevedev-local:8443/login/oauth2/code/auth0-org`

### Requirement: Existing direct and Keycloak-federated login flows are unaffected
Adding the `auth0-org` registration and extending `ConnectionAwareRequestResolver` SHALL NOT change the behaviour of the `auth0` or `auth0-keycloak` flows.

#### Scenario: Direct Auth0 login still works after the change
- **WHEN** a user clicks "Login direct (Auth0)"
- **THEN** the authorize request SHALL contain `connection=<OAUTH2_CONNECTION_DIRECT>` and SHALL NOT contain an `organization` parameter

#### Scenario: Keycloak-federated login still works after the change
- **WHEN** a user clicks "Login federated (Auth0 → Keycloak)"
- **THEN** the authorize request SHALL contain `connection=<OAUTH2_CONNECTION_KEYCLOAK>` and SHALL NOT contain an `organization` parameter
