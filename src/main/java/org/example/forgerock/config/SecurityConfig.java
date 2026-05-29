package org.example.forgerock.config;

import java.nio.charset.StandardCharsets;
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
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // 2.7.x style:
                .authorizeRequests(auth -> auth
                        .antMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/Designer.png").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
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
        if (authentication instanceof OAuth2AuthenticationToken) {
            Object principal = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            if (principal instanceof OidcUser) {
                builder.queryParam("id_token_hint",
                        ((OidcUser) principal).getIdToken().getTokenValue());
            }
        }
        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }
}