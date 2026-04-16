package org.example.forgerock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${app.post-logout-redirect-uri:http://localhost:8080/}")
    private String postLogoutRedirectUri;

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
                .logout(logout -> {
                    // RP-initiated logout if IdP supports end_session_endpoint
                    OidcClientInitiatedLogoutSuccessHandler oidcLogoutHandler =
                            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
                    oidcLogoutHandler.setPostLogoutRedirectUri(postLogoutRedirectUri);
                    logout.logoutSuccessHandler(oidcLogoutHandler);
                });

        return http.build();
    }
}