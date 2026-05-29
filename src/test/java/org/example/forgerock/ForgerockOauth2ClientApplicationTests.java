package org.example.forgerock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.auth0.client-id=test-id",
    "spring.security.oauth2.client.registration.auth0.client-secret=test-secret",
    "spring.security.oauth2.client.registration.auth0.redirect-uri=https://localhost/callback",
    "spring.security.oauth2.client.provider.auth0.issuer-uri=https://mock-issuer.example.com"
})
class ForgerockOauth2ClientApplicationTests {

    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }

}
