## ADDED Requirements

### Requirement: Authenticated user can invoke the OIDC UserInfo endpoint from the UI
The application SHALL expose a page at `/api-calls` (authenticated) that, when loaded, makes a server-side GET request to the configured OIDC provider's UserInfo endpoint using the current session's access token and displays the raw JSON response.

#### Scenario: UserInfo call succeeds
- **WHEN** an authenticated user navigates to `/api-calls`
- **THEN** the application SHALL call the UserInfo endpoint with an `Authorization: Bearer <access_token>` header and display the HTTP status code and raw JSON response body on the page

#### Scenario: UserInfo call fails with a provider error
- **WHEN** the UserInfo endpoint returns a non-2xx HTTP status
- **THEN** the application SHALL display the HTTP status code and the response body (error description) rather than throwing an unhandled exception

#### Scenario: Access token is expired when the page is loaded
- **WHEN** the access token has expired and a refresh token is available
- **THEN** the application SHALL refresh the token transparently before making the UserInfo call

#### Scenario: Session is expired (no valid token obtainable)
- **WHEN** the access token has expired and no refresh token is available
- **THEN** the application SHALL display a "session expired — please log in again" message and a link to `/`

### Requirement: Token introspection call is available when an introspection URI is configured
The application SHALL display a token introspection panel on the `/api-calls` page when `APP_INTROSPECTION_URI` is set, making a POST to the introspection endpoint with the access token and client credentials.

#### Scenario: Introspection URI is configured and call succeeds
- **WHEN** `APP_INTROSPECTION_URI` is set and an authenticated user loads `/api-calls`
- **THEN** the application SHALL POST to the introspection URI with `token=<access_token>` and HTTP Basic credentials (`client_id`/`client_secret`) and display the JSON response

#### Scenario: Introspection URI is not configured
- **WHEN** `APP_INTROSPECTION_URI` is not set
- **THEN** the `/api-calls` page SHALL NOT display the introspection panel

#### Scenario: Introspection call returns an HTTP error
- **WHEN** the introspection endpoint returns a non-2xx HTTP status
- **THEN** the application SHALL display the HTTP status code and response body rather than throwing an unhandled exception

### Requirement: `/api-calls` page is only accessible to authenticated users
The `/api-calls` endpoint SHALL be protected and redirect unauthenticated users to the OAuth2 login flow.

#### Scenario: Unauthenticated access to /api-calls
- **WHEN** an unauthenticated user navigates to `/api-calls`
- **THEN** the application SHALL redirect to the OAuth2 provider's authorisation endpoint
