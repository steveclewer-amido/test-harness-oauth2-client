# test-harness-oauth2-client

This project demonstrates a Spring Boot application acting as an OAuth2 client for ForgeRock Identity Platform. It provides a simple web interface to authenticate users via ForgeRock, display their ID and access tokens, and supports secure logout.

## Features

- OAuth2 Authorization Code flow with ForgeRock as the provider
- HTTPS enabled by default (self-signed keystore included)
- Thymeleaf-based UI with JSON claims highlighting
- Secure login and logout flows
- Live API call page — invoke the OIDC UserInfo endpoint and (optionally) token introspection directly from the browser using the current session's tokens
- Automatic access token refresh via refresh token grant before outbound API calls
- Configurable via `application.yml`

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- A ForgeRock AM instance with an OAuth2 client registered
- The client must allow the redirect URI used by this app (see below)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/forgerock-oauth2-client.git
cd forgerock-oauth2-client
```

### 2. Configure Application

The following environment variables are required at startup:

| Variable | Required | Description | Example |
|---|---|---|---|
| `OAUTH2_CLIENT_ID` | Yes | OAuth2 client ID registered with ForgeRock | `Testharness` |
| `OAUTH2_CLIENT_SECRET` | Yes | OAuth2 client secret | `<your-secret>` |
| `OAUTH2_ISSUER_URI` | Yes | OIDC issuer URI for the ForgeRock realm | `https://<host>/am/oauth2/alpha` |
| `APP_INTROSPECTION_URI` | No | Token introspection endpoint. When set, enables the introspection panel on the API calls page. Uses `client_secret_post` auth. | `https://<host>/am/oauth2/alpha/introspect` |

Set them in your shell before running:

```bash
export OAUTH2_CLIENT_ID=Testharness
export OAUTH2_CLIENT_SECRET=<your-secret>
export OAUTH2_ISSUER_URI=https://<your-fr-host>/am/oauth2/alpha
```

Or pass them inline:

```bash
OAUTH2_CLIENT_ID=Testharness \
OAUTH2_CLIENT_SECRET=<your-secret> \
OAUTH2_ISSUER_URI=https://<your-fr-host>/am/oauth2/alpha \
APP_INTROSPECTION_URI=https://<your-fr-host>/am/oauth2/alpha/introspect \
mvn spring-boot:run
```

The `redirect-uri` is set to `https://stevedev-local:8443/login/oauth2/code/forgerock` and must match one of the allowed redirect URIs in your ForgeRock client registration. To change it, update `application.yml`.

The default HTTPS keystore is `keystore.p12` with password `password` (for development only).

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The app will start on `https://stevedev-local:8443/`.

### 4. Access the App

Open [https://stevedev-local:8443/](https://stevedev-local:8443/) in your browser. You may need to trust the self-signed certificate.

### 5. Login with ForgeRock

Click the login button to authenticate via ForgeRock. After login, your ID and access token claims will be displayed with syntax highlighting.

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

- **403 or redirect_uri_mismatch**: Ensure the `redirect-uri` in `application.yml` matches exactly with the value registered in ForgeRock.
- **Blank login page**: Check browser console for cookie or CORS issues. Ensure your hostname is resolvable and trusted by ForgeRock.
- **HTTPS issues**: Trust the self-signed certificate or use a valid certificate for your environment.
- **401 on introspection**: Ensure the ForgeRock client is configured with `client_secret_post` as the token endpoint authentication method, which matches the `client-authentication-method` setting in `application.yml`.
- **Introspection panel not shown**: Confirm `APP_INTROSPECTION_URI` is set in the environment before starting the app. The panel is hidden when the variable is absent or empty.

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
