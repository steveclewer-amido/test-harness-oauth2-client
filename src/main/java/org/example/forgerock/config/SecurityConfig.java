package org.example.forgerock.config;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class SecurityConfig {

    @Value("${app.post-logout-redirect-uri:http://localhost:8080/}")
    private String postLogoutRedirectUri;

    @Value("${spring.security.oauth2.client.registration.auth0.client-id}")
    private String auth0ClientId;

    @Value("${spring.security.oauth2.client.provider.auth0.issuer-uri}")
    private String auth0IssuerUri;

    @Value("${app.connections.auth0:Username-Password-Authentication}")
    private String auth0Connection;

    @Value("${app.connections.auth0-keycloak:keycloak-alpha}")
    private String auth0KeycloakConnection;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // 2.7.x style:
                .authorizeRequests(auth -> auth
                        .antMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/Designer.png").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        .authorizationEndpoint(authz -> authz
                                .authorizationRequestResolver(
                                        new ConnectionAwareRequestResolver(
                                                clientRegistrationRepository,
                                                Map.of("auth0", auth0Connection,
                                                       "auth0-keycloak", auth0KeycloakConnection)))))
                .oauth2Client(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        // 2.7.x style for CSRF ignore
                        .ignoringAntMatchers("/actuator/**")
                )
                .logout(logout -> logout.logoutSuccessHandler(auth0LogoutSuccessHandler()));

        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private LogoutSuccessHandler auth0LogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String url = buildLogoutUrl(authentication);
            new DefaultRedirectStrategy().sendRedirect(request, response, url);
        };
    }

    private String buildLogoutUrl(Authentication authentication) {
        String base = auth0IssuerUri.endsWith("/") ? auth0IssuerUri : auth0IssuerUri + "/";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(base + "oidc/logout")
                .queryParam("client_id", auth0ClientId)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
        if (authentication instanceof OAuth2AuthenticationToken oat) {
            Object principal = oat.getPrincipal();
            if (principal instanceof OidcUser) {
                builder.queryParam("id_token_hint",
                        ((OidcUser) principal).getIdToken().getTokenValue());
            }
            // Propagate logout to the upstream IdP (e.g. Keycloak) when the user
            // authenticated via a federated connection. Auth0 supports a 'federated'
            // parameter on its logout endpoint for exactly this purpose.
            if ("auth0-keycloak".equals(oat.getAuthorizedClientRegistrationId())) {
                builder.queryParam("federated", "");
            }
        }
        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    /**
     * Adds the Auth0 {@code connection} query parameter to the authorization request
     * for each registration, without embedding it in the {@code authorization-uri}.
     * Spring Security requires {@code authorization-uri} to be clean (no query params);
     * additional OAuth parameters must go through this resolver.
     */
    private static final class ConnectionAwareRequestResolver implements OAuth2AuthorizationRequestResolver {

        private final DefaultOAuth2AuthorizationRequestResolver delegate;
        private final Map<String, String> connectionsByRegistrationId;

        ConnectionAwareRequestResolver(ClientRegistrationRepository repo,
                                       Map<String, String> connectionsByRegistrationId) {
            this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
            this.connectionsByRegistrationId = connectionsByRegistrationId;
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            OAuth2AuthorizationRequest base = delegate.resolve(request);
            return withConnection(base, extractRegistrationId(request));
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
            OAuth2AuthorizationRequest base = delegate.resolve(request, clientRegistrationId);
            return withConnection(base, clientRegistrationId);
        }

        private OAuth2AuthorizationRequest withConnection(OAuth2AuthorizationRequest base, String registrationId) {
            if (base == null || registrationId == null) {
                return base;
            }
            String connection = connectionsByRegistrationId.get(registrationId);
            if (connection == null) {
                return base;
            }
            Map<String, Object> params = new HashMap<>(base.getAdditionalParameters());
            params.put("connection", connection);
            return OAuth2AuthorizationRequest.from(base)
                    .additionalParameters(params)
                    .build();
        }

        private static String extractRegistrationId(HttpServletRequest request) {
            String uri = request.getRequestURI();
            int lastSlash = uri.lastIndexOf('/');
            return lastSlash >= 0 ? uri.substring(lastSlash + 1) : null;
        }
    }
}