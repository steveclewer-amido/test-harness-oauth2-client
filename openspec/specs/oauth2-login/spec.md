### Requirement: Application redirects unauthenticated users to OAuth2 provider login
The application SHALL redirect any unauthenticated request for a protected resource to the configured OAuth2 provider's authorisation endpoint using the Authorization Code flow, including a `state` parameter and (where supported) PKCE challenge.

#### Scenario: Unauthenticated user accesses a protected page
- **WHEN** an unauthenticated user navigates to `/tokens`
- **THEN** the application SHALL redirect the browser to the OAuth2 provider's authorisation endpoint

#### Scenario: OAuth2 provider redirects back with authorisation code
- **WHEN** the OAuth2 provider redirects back to the application's callback URI with a valid `code` and matching `state`
- **THEN** the application SHALL exchange the code for tokens and establish an authenticated session

#### Scenario: Callback with invalid state parameter
- **WHEN** the OAuth2 provider redirects back with a `state` that does not match the stored value
- **THEN** the application SHALL reject the callback and return an error response (HTTP 4xx)

### Requirement: Application exposes a public landing page
The application SHALL serve a public landing page at `/` that is accessible without authentication and includes a link to initiate login.

#### Scenario: Public home page is accessible without login
- **WHEN** a user (authenticated or not) requests `/`
- **THEN** the application SHALL return HTTP 200 with the landing page HTML

### Requirement: OAuth2 provider registration is configurable without code changes
The application SHALL read `client-id`, `client-secret`, `authorization-uri`, `token-uri`, and `jwk-set-uri` from environment variables so that the target OAuth2 provider can be changed without modifying source code.

#### Scenario: Application starts with required environment variables set
- **WHEN** `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`, and provider URIs are set as environment variables
- **THEN** the application SHALL start successfully and be ready to perform the OAuth2 flow

#### Scenario: Application starts without required environment variables
- **WHEN** required OAuth2 environment variables are absent
- **THEN** the application SHALL fail to start with a descriptive configuration error
