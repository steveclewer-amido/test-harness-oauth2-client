package org.example.forgerock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class ApiCallService {

    public record ApiCallResult(int statusCode, String body) {}

    private final OAuth2AuthorizedClientManager clientManager;
    private final RestTemplate restTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${app.introspection-uri:}")
    private String introspectionUri;

    public ApiCallService(OAuth2AuthorizedClientManager clientManager,
                          RestTemplate restTemplate,
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.clientManager = clientManager;
        this.restTemplate = restTemplate;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public ApiCallResult callUserInfo(Authentication auth,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        OAuth2AuthorizedClient client = authorize(auth, request, response);
        if (client == null) {
            return new ApiCallResult(0, "Session expired — please log in again.");
        }

        String userInfoUri = client.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri();
        if (!StringUtils.hasText(userInfoUri)) {
            return new ApiCallResult(0, "UserInfo endpoint URI is not configured for this provider.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(client.getAccessToken().getTokenValue());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    userInfoUri, HttpMethod.GET, entity, String.class);
            return new ApiCallResult(resp.getStatusCodeValue(), resp.getBody());
        } catch (HttpStatusCodeException ex) {
            return new ApiCallResult(ex.getRawStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return new ApiCallResult(0, "Request failed: " + ex.getMessage());
        }
    }

    /**
     * Returns null when introspection URI is not configured (callers hide the panel).
     */
    public ApiCallResult callIntrospection(Authentication auth,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        if (!StringUtils.hasText(introspectionUri)) {
            return null;
        }

        OAuth2AuthorizedClient client = authorize(auth, request, response);
        if (client == null) {
            return new ApiCallResult(0, "Session expired — please log in again.");
        }

        ClientRegistration reg = client.getClientRegistration();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", client.getAccessToken().getTokenValue());
        // Use client_secret_post (credentials in body) to match the registered
        // client-authentication-method in application.yml.
        form.add("client_id", reg.getClientId());
        form.add("client_secret", reg.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(introspectionUri, entity, String.class);
            return new ApiCallResult(resp.getStatusCodeValue(), resp.getBody());
        } catch (HttpStatusCodeException ex) {
            return new ApiCallResult(ex.getRawStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return new ApiCallResult(0, "Request failed: " + ex.getMessage());
        }
    }

    private OAuth2AuthorizedClient authorize(Authentication auth,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("auth0")
                .principal(auth)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build();
        return clientManager.authorize(authorizeRequest);
    }
}
