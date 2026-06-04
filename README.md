# test-harness-oauth2-client

This project demonstrates a Spring Boot application acting as an OAuth2 client for Auth0. It provides a simple web interface to authenticate users via Auth0 (optionally federated through Keycloak), display their ID and access tokens, and supports secure logout.

## Features

- OAuth2 Authorization Code flow with Auth0 as the provider
- Three login paths: direct Auth0 (`Username-Password-Authentication` connection), Auth0 federated via Keycloak, and Auth0 scoped to a specific organization
- `ConnectionAwareRequestResolver` injects Auth0 `connection` and `organization` parameters at authorization time without embedding them in `authorization-uri`
- Federated logout: when signed in via the Keycloak connection, logout propagates upstream using Auth0's `federated` parameter
- HTTPS enabled by default (self-signed keystore included)
- Thymeleaf-based UI with JSON claims highlighting
- Secure login and logout flows
- Live API call page — invoke the OIDC UserInfo endpoint and (optionally) token introspection directly from the browser using the current session's tokens
- Automatic access token refresh via refresh token grant before outbound API calls
- Configurable via `application.yml`

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- An Auth0 tenant with an application registered (Regular Web Application)
- The application must allow the redirect URIs used by this app (see below)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/test-harness-oauth2-client.git
cd test-harness-oauth2-client
```

### 2. Configure Application

The following environment variables are required at startup:

| Variable | Required | Description | Example |
|---|---|---|---|
| `OAUTH2_CLIENT_ID` | Yes | OAuth2 client ID registered with Auth0 | `abc123` |
| `OAUTH2_CLIENT_SECRET` | Yes | OAuth2 client secret | `<your-secret>` |
| `OAUTH2_ISSUER_URI` | Yes | OIDC issuer URI for your Auth0 tenant | `https://<tenant>.auth0.com/` |
| `APP_INTROSPECTION_URI` | No | Token introspection endpoint. When set, enables the introspection panel on the API calls page. Uses `client_secret_post` auth. | `https://<tenant>.auth0.com/oauth/introspect` |
| `OAUTH2_CONNECTION_DIRECT` | No | Auth0 connection name for the direct login path. Defaults to `Username-Password-Authentication`. | `Username-Password-Authentication` |
| `OAUTH2_CONNECTION_KEYCLOAK` | No | Auth0 connection name for the federated Keycloak login path. Defaults to `keycloak-alpha`. | `keycloak-alpha` |
| `OAUTH2_ORG_ID` | No | Auth0 organization ID. When set, enables the organization login path which passes `organization=<id>` to the authorize endpoint. | `org_HIEqnJwKqn595kRF` |

Set them in your shell before running:

```bash
export OAUTH2_CLIENT_ID=<your-client-id>
export OAUTH2_CLIENT_SECRET=<your-secret>
export OAUTH2_ISSUER_URI=https://<your-auth0-tenant>.auth0.com/
```

Or pass them inline:

```bash
OAUTH2_CLIENT_ID=<your-client-id> \
OAUTH2_CLIENT_SECRET=<your-secret> \
OAUTH2_ISSUER_URI=https://<your-auth0-tenant>.auth0.com/ \
APP_INTROSPECTION_URI=https://<your-auth0-tenant>.auth0.com/oauth/introspect \
OAUTH2_ORG_ID=<your-org-id> \
mvn spring-boot:run
```

The redirect URIs are:

- `https://stevedev-local:8443/login/oauth2/code/auth0` (direct Auth0 login)
- `https://stevedev-local:8443/login/oauth2/code/auth0-keycloak` (Keycloak federated login)
- `https://stevedev-local:8443/login/oauth2/code/auth0-org` (organization login)

All three must be registered as **Allowed Callback URLs** in your Auth0 application. To change them, update `application.yml`.

The default HTTPS keystore is `keystore.p12` with password `password` (for development only).

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The app will start on `https://stevedev-local:8443/`.

### 4. Access the App

Open [https://stevedev-local:8443/](https://stevedev-local:8443/) in your browser. You may need to trust the self-signed certificate.

### 5. Login with Auth0

The home page presents three login buttons:

- **Login direct (Auth0)** — authenticates directly via the `Username-Password-Authentication` connection (or the value of `OAUTH2_CONNECTION_DIRECT`).
- **Login federated (Auth0 → Keycloak)** — routes through the `keycloak-alpha` connection (or the value of `OAUTH2_CONNECTION_KEYCLOAK`), delegating authentication upstream to Keycloak.
- **Login with Organization (Auth0)** — passes `organization=<OAUTH2_ORG_ID>` to Auth0's authorize endpoint, scoping the login to a specific Auth0 organization. Only appears useful when `OAUTH2_ORG_ID` is set.

After login, your ID and access token claims will be displayed with syntax highlighting.

### 6. Make API Calls

From the token display page, click **Make API Calls** to open `/api-calls`. This page:

- Calls the OIDC **UserInfo endpoint** using your current access token and displays the raw JSON response.
- If `APP_INTROSPECTION_URI` is set, also calls the **token introspection endpoint** (using `client_secret_post` credentials) and displays the result.
- Automatically refreshes an expired access token via the refresh token grant before making calls.
- Displays the HTTP status code alongside each response — useful for debugging scope or permission issues.

### 7. Logout

Click the logout button to end your session and be redirected as configured.

## Customization

- **Keystore**: Replace `keystore.p12` with your own certificate for production use.
- **Scopes**: Add or remove scopes in `application.yml` as needed.
- **Logout Redirect**: Set `app.post-logout-redirect-uri` in `application.yml` to control where users are sent after logout.

## Troubleshooting

- **403 or redirect_uri_mismatch**: Ensure all three redirect URIs (`/login/oauth2/code/auth0`, `/login/oauth2/code/auth0-keycloak`, and `/login/oauth2/code/auth0-org`) are registered as Allowed Callback URLs in your Auth0 application.
- **Blank login page**: Check browser console for cookie or CORS issues. Ensure your hostname is resolvable and trusted by Auth0.
- **HTTPS issues**: Trust the self-signed certificate or use a valid certificate for your environment.
- **401 on introspection**: Ensure the Auth0 application is configured with `client_secret_post` as the token endpoint authentication method, which matches the `client-authentication-method` setting in `application.yml`.
- **Introspection panel not shown**: Confirm `APP_INTROSPECTION_URI` is set in the environment before starting the app. The panel is hidden when the variable is absent or empty.
- **Keycloak federated login not working**: Confirm the Auth0 connection name matches the value of `OAUTH2_CONNECTION_KEYCLOAK` and that `https://stevedev-local:8443/login/oauth2/code/auth0-keycloak` is an Allowed Callback URL.
- **Federated logout not propagating**: Auth0's `federated` logout parameter is only sent when the user authenticated via the `auth0-keycloak` registration. Confirm the Keycloak connection in Auth0 has back-channel logout or OIDC RP-Initiated Logout configured.
- **Organization login not working**: Confirm `OAUTH2_ORG_ID` is set to a valid Auth0 organization ID (e.g. `org_...`) and that `https://stevedev-local:8443/login/oauth2/code/auth0-org` is an Allowed Callback URL. Also ensure the Auth0 application is enabled for the organization in the Auth0 dashboard.

## License

This project is provided as an example and is not intended for production use without further security review.

---

## Development Workflow (OpenSpec)

This project uses [openspec](https://openspec.dev) to manage change proposals, design decisions, specs, and implementation tasks.

### Prerequisites

Install the openspec CLI:

```bash
npm install -g openspec
```

### Key Commands

| Command | Description |
|---|---|
| `openspec list` | List all changes and their status |
| `openspec status --change <name>` | Show artifact and task progress for a change |
| `openspec new change <name>` | Scaffold a new change with proposal/design/specs/tasks |
| `openspec instructions <artifact> --change <name> --json` | Get AI instructions for creating an artifact |
| `openspec instructions apply --change <name> --json` | Get instructions and task list for implementation |

### VS Code Shortcuts

The `.github/prompts/` directory contains slash-command shortcuts for use with GitHub Copilot Chat:

| Command | Description |
|---|---|
| `/opsx:propose` | Propose a new change — generates proposal, design, specs, and tasks in one step |
| `/opsx:apply` | Implement pending tasks from an active change |
| `/opsx:archive` | Archive a completed change |
| `/opsx:explore` | Enter explore/thinking mode before or during a change |

### Change Structure

Changes live under `openspec/changes/<name>/`:

```
openspec/changes/<name>/
  proposal.md   # Why — motivation and capabilities
  design.md     # How — technical decisions and trade-offs
  specs/
    <capability>/spec.md  # What — testable requirements and scenarios
  tasks.md      # Implementation checklist
```

### Example: How this project was built

```bash
# Propose the OAuth2 browser auth feature
/opsx:propose

# Implement the tasks (existing code was already in place)
/opsx:apply

# Archive when done
/opsx:archive
```
