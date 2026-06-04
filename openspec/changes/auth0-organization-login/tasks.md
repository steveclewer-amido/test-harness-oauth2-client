## 1. Configuration

- [x] 1.1 Add `auth0-org` registration block to `application.yml` (same `client-id`/`client-secret`, `provider: auth0-org`, `redirect-uri: https://stevedev-local:8443/login/oauth2/code/auth0-org`, scopes `openid profile email`)
- [x] 1.2 Add `auth0-org` provider block to `application.yml` (`issuer-uri: ${OAUTH2_ISSUER_URI}`)
- [x] 1.3 Add `app.organizations.auth0-org: ${OAUTH2_ORG_ID:}` to `application.yml`

## 2. SecurityConfig — ConnectionAwareRequestResolver

- [x] 2.1 Add `@Value("${app.organizations.auth0-org:}")` field `auth0OrgId` to `SecurityConfig`
- [x] 2.2 Add `organizationsByRegistrationId` field and constructor parameter to `ConnectionAwareRequestResolver`
- [x] 2.3 Update `withConnection` to also inject `organization` from `organizationsByRegistrationId` when non-empty, independently of `connection`
- [x] 2.4 Update `securityFilterChain` to pass `Map.of("auth0-org", auth0OrgId)` as the second map argument to `ConnectionAwareRequestResolver`

## 3. HomeController

- [x] 3.1 Add `@Value("${spring.security.oauth2.client.registration.auth0-org.redirect-uri}")` field `redirectUriOrg`
- [x] 3.2 Add `model.addAttribute("redirectUriOrg", redirectUriOrg)` to the `GET /` handler

## 4. index.html

- [x] 4.1 Add "Login with Organization (Auth0)" button linking to `/oauth2/authorization/auth0-org`
- [x] 4.2 Add `redirectUriOrg` to the redirect URI display section

## 5. README

- [x] 5.1 Add `OAUTH2_ORG_ID` row to the environment variable table
- [x] 5.2 Add `auth0-org` redirect URI to the Allowed Callback URLs list
- [x] 5.3 Document the third login button in the login steps section
- [x] 5.4 Add `OAUTH2_ORG_ID` to the inline run example
- [x] 5.5 Update the `redirect_uri_mismatch` troubleshooting entry to list all three URIs
- [x] 5.6 Add "Organization login not working" troubleshooting entry
- [x] 5.7 Update features list to reference three login paths

## 6. Validation

- [x] 6.1 Build the project (`./mvnw clean package -q`) and confirm it compiles without errors
- [ ] 6.2 Manually verify: with `OAUTH2_ORG_ID` set, clicking "Login with Organization" redirects to Auth0 with `organization=<org-id>` in the authorize URL
- [ ] 6.3 Manually verify: after successful organization login, the ID token contains `org_id` and/or `org_name` claims displayed on `/home`
- [ ] 6.4 Manually verify: direct Auth0 login and Keycloak-federated login still work as before (no `organization` parameter in their authorize requests)
- [ ] 6.5 Manually verify: with `OAUTH2_ORG_ID` unset, the app starts and the "Login with Organization" button is present but sends no `organization` parameter
