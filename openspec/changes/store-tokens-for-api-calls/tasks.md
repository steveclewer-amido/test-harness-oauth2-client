## 1. Configuration & Beans

- [x] 1.1 Register a `DefaultOAuth2AuthorizedClientManager` bean in `SecurityConfig`, composing `OAuth2AuthorizedClientRepository` and a provider chain that includes `RefreshTokenOAuth2AuthorizedClientProvider`
- [x] 1.2 Register a `RestTemplate` bean (plain, no interceptors) in `SecurityConfig` for use by `ApiCallService`
- [x] 1.3 Add `app.introspection-uri` optional property binding in `application.yml` (maps to `APP_INTROSPECTION_URI` environment variable)

## 2. ApiCallService

- [x] 2.1 Create `src/main/java/org/example/forgerock/service/ApiCallService.java` — a `@Service` that accepts `OAuth2AuthorizedClientManager`, `RestTemplate`, and `ClientRegistrationRepository`
- [x] 2.2 Implement `callUserInfo(Authentication auth, HttpServletRequest request)` — resolves the authorized client via manager, derives the UserInfo URI from `ClientRegistration.getProviderDetails().getUserInfoEndpoint().getUri()`, makes a GET with `Authorization: Bearer`, returns an `ApiCallResult` record (statusCode, body)
- [x] 2.3 Implement `callIntrospection(Authentication auth, HttpServletRequest request, String tokenValue)` — POSTs `token=<value>` to `APP_INTROSPECTION_URI` with HTTP Basic client credentials; returns `ApiCallResult`; returns a "not configured" result when the URI is absent
- [x] 2.4 Handle the case where `OAuth2AuthorizedClientManager.authorize()` returns `null` (expired session) — return an `ApiCallResult` with a descriptive error message rather than throwing

## 3. ApiCallsController

- [x] 3.1 Create `src/main/java/org/example/forgerock/web/ApiCallsController.java` — a `@Controller` mapped to `/api-calls`
- [x] 3.2 Inject `ApiCallService` and `@Value("${app.introspection-uri:}")` into the controller
- [x] 3.3 Implement `GET /api-calls` — call `ApiCallService.callUserInfo()`, conditionally call `callIntrospection()`, add results to `Model`, render `api-calls` view
- [x] 3.4 Add `/api-calls` to the permitted request matchers list in `SecurityConfig` — it must remain authenticated (not public); verify it is covered by `.anyRequest().authenticated()`

## 4. Thymeleaf Template

- [x] 4.1 Create `src/main/resources/templates/api-calls.html` — authenticated page with two panels: "UserInfo Response" and (conditionally) "Token Introspection Response", each showing status code and formatted JSON body
- [x] 4.2 Add a "Back to tokens" link to `/home` and a logout form on `api-calls.html`

## 5. Navigation Update

- [x] 5.1 Add a "Make API Calls" link to `/api-calls` in `src/main/resources/templates/home.html`

## 6. Validation

- [x] 6.1 Build the project (`./mvnw clean package -q`) and confirm it compiles without errors
- [ ] 6.2 Manually verify: log in, navigate to `/api-calls`, confirm UserInfo JSON is displayed
- [ ] 6.3 Manually verify: with `APP_INTROSPECTION_URI` unset, confirm the introspection panel is hidden
- [ ] 6.4 Manually verify: with `APP_INTROSPECTION_URI` set, confirm the introspection panel shows a result
