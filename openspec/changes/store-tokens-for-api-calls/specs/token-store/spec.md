## ADDED Requirements

### Requirement: Application provides a session-scoped authorized client with automatic token refresh
The application SHALL use `OAuth2AuthorizedClientManager` to retrieve the current session's `OAuth2AuthorizedClient`, transparently refreshing the access token via the refresh token grant when the token has expired.

#### Scenario: Access token is still valid
- **WHEN** `OAuth2AuthorizedClientManager.authorize()` is called and the current access token has not expired
- **THEN** the manager SHALL return the existing `OAuth2AuthorizedClient` unchanged

#### Scenario: Access token is expired and a refresh token is present
- **WHEN** `OAuth2AuthorizedClientManager.authorize()` is called and the access token has expired but a refresh token is available
- **THEN** the manager SHALL exchange the refresh token for a new access token and return an updated `OAuth2AuthorizedClient`

#### Scenario: Access token is expired and no refresh token is present
- **WHEN** `OAuth2AuthorizedClientManager.authorize()` is called and the access token has expired with no refresh token
- **THEN** the manager SHALL return `null` and the calling code SHALL surface a "session expired" error to the user rather than throwing an unchecked exception
