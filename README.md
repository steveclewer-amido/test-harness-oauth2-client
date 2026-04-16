# test-harness-oauth2-client

This project demonstrates a Spring Boot application acting as an OAuth2 client for ForgeRock Identity Platform. It provides a simple web interface to authenticate users via ForgeRock, display their ID and access tokens, and supports secure logout.

## Features

- OAuth2 Authorization Code flow with ForgeRock as the provider
- HTTPS enabled by default (self-signed keystore included)
- Thymeleaf-based UI with JSON claims highlighting
- Secure login and logout flows
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

Edit `src/main/resources/application.yml` to set your ForgeRock AM issuer, client ID, and client secret. Example:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          forgerock:
            client-id: XXX
            client-secret: XXX 
            redirect-uri: "https://stevedev-local:8443/login/oauth2/code/forgerock"
        provider:
          forgerock:
            issuer-uri: https://XXX/am/oauth2/alpha
```

- The `redirect-uri` must match one of the allowed redirect URIs in your ForgeRock client registration.
- The default HTTPS keystore is `keystore.p12` with password `password` (for development only).

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The app will start on `https://stevedev-local:8443/`.

### 4. Access the App

Open [https://stevedev-local:8443/](https://stevedev-local:8443/) in your browser. You may need to trust the self-signed certificate.

### 5. Login with ForgeRock

Click the login button to authenticate via ForgeRock. After login, your ID and access token claims will be displayed with syntax highlighting.

### 6. Logout

Click the logout button to end your session and be redirected as configured.

## Customization

- **Keystore**: Replace `keystore.p12` with your own certificate for production use.
- **Scopes**: Add or remove scopes in `application.yml` as needed.
- **Logout Redirect**: Set `app.post-logout-redirect-uri` in `application.yml` to control where users are sent after logout.

## Troubleshooting

- **403 or redirect_uri_mismatch**: Ensure the `redirect-uri` in `application.yml` matches exactly with the value registered in ForgeRock.
- **Blank login page**: Check browser console for cookie or CORS issues. Ensure your hostname is resolvable and trusted by ForgeRock.
- **HTTPS issues**: Trust the self-signed certificate or use a valid certificate for your environment.

## License

This project is provided as an example and is not intended for production use without further security review.
