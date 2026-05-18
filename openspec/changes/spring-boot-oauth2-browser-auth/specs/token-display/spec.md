## ADDED Requirements

### Requirement: Authenticated user can view their tokens at /tokens
After a successful OAuth2 login the application SHALL serve a page at `/tokens` that displays the access token, ID token (if present), and refresh token (if present) for the authenticated user.

#### Scenario: Authenticated user requests /tokens
- **WHEN** an authenticated user navigates to `/tokens`
- **THEN** the application SHALL return HTTP 200 with a page containing the access token value

#### Scenario: ID token is present
- **WHEN** the OAuth2 provider returned an ID token during the authorization flow
- **THEN** the `/tokens` page SHALL display the raw ID token string

#### Scenario: Refresh token is present
- **WHEN** the OAuth2 provider returned a refresh token during the authorization flow
- **THEN** the `/tokens` page SHALL display the raw refresh token string

#### Scenario: Refresh token is absent
- **WHEN** the OAuth2 provider did not return a refresh token
- **THEN** the `/tokens` page SHALL indicate that no refresh token was issued rather than rendering a blank field

### Requirement: Token display page shows authenticated principal attributes
The `/tokens` page SHALL display the principal name and all available OAuth2 user attributes (claims) returned by the provider, alongside the token values.

#### Scenario: User attributes are available
- **WHEN** the OAuth2 provider returns user info claims (e.g., `sub`, `email`, `name`)
- **THEN** the `/tokens` page SHALL display each claim name and its value

### Requirement: Token display page includes a sign-out link
The `/tokens` page SHALL include a logout action that invalidates the session and clears the security context.

#### Scenario: User clicks sign out
- **WHEN** the authenticated user submits the logout action from the `/tokens` page
- **THEN** the application SHALL invalidate the session and redirect the browser to the public landing page `/`
