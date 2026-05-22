## MODIFIED Requirements

### Requirement: Token display page includes a sign-out link
The `/tokens` page (served at `/home`) SHALL include a logout action that invalidates the session and clears the security context, AND SHALL include a navigation link to the `/api-calls` page.

#### Scenario: User clicks sign out
- **WHEN** the authenticated user submits the logout action from the `/home` page
- **THEN** the application SHALL invalidate the session and redirect the browser to the public landing page `/`

#### Scenario: Navigation link to API calls page is present
- **WHEN** an authenticated user views the `/home` page
- **THEN** the page SHALL display a link labelled "Make API Calls" that navigates to `/api-calls`
