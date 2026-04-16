package org.example.forgerock.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Controller
public class HomeController {

    @Value("${spring.security.oauth2.client.registration.forgerock.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.forgerock.issuer-uri}")
    private String issuerUri;

    private final Environment env;
    private final ObjectMapper mapper;

    public HomeController(Environment env) {
        this.env = env;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("issuerUri", issuerUri);
        model.addAttribute("redirectUri", redirectUri);

        // If already authenticated, jump to the landing page
        if (oidcUser != null) {
            return "redirect:/home";
        }
        return "index";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OidcUser oidcUser,
                       @RegisteredOAuth2AuthorizedClient("forgerock") OAuth2AuthorizedClient client,
                       Model model) {
        // Basic principal info (from ID Token if openid scope is present)
        if (oidcUser != null) {
            model.addAttribute("userName", oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getName());
            model.addAttribute("userSubject", oidcUser.getSubject());
            model.addAttribute("idTokenClaims", oidcUser.getClaims());


            // Pretty JSON for ID token claims
            String prettyIdJson = toPrettyJson(oidcUser.getClaims());
            System.err.println("DEBUG idTokenClaimsJson ->\n" + prettyIdJson);   // <<< debug

            // Pretty JSON for ID token claims
            model.addAttribute("idTokenClaimsJson", toPrettyJson(oidcUser.getClaims()));
        }

        // Access Token and metadata
        String accessToken = client.getAccessToken().getTokenValue();
        Instant issuedAt = client.getAccessToken().getIssuedAt();
        Instant expiresAt = client.getAccessToken().getExpiresAt();
        model.addAttribute("accessToken", accessToken);
        model.addAttribute("accessTokenIssuedAt", issuedAt);
        model.addAttribute("accessTokenExpiresAt", expiresAt);
        model.addAttribute("accessTokenScopes", client.getAccessToken().getScopes());

        // Decode access token & pretty print claims (and raw payload if present)
        Map<String, Object> accessTokenClaims = tryDecodeAccessToken(accessToken);

        // If we produced a raw payload string, try to pretty-format it too
        Object rawPayload = accessTokenClaims.get("_raw_payload_json");
        if (rawPayload instanceof String) {
            String prettyRaw = toPrettyJson((String) rawPayload);
            if (prettyRaw != null) {
                accessTokenClaims.put("_raw_payload_json", prettyRaw);
            }
        }

        // Try to decode Access Token as JWT and extract claims
        model.addAttribute("accessTokenClaims", accessTokenClaims);
        model.addAttribute("accessTokenClaimsJson", toPrettyJson(accessTokenClaims));

        return "home";
    }

    private Map<String, Object> tryDecodeAccessToken(String token) {
        Map<String, Object> claims = new LinkedHashMap<>();
        String issuer = env.getProperty("spring.security.oauth2.client.provider.forgerock.issuer-uri");
        if (issuer != null) {
            try {
                JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
                Jwt jwt = decoder.decode(token);
                claims.putAll(jwt.getClaims());
                claims.put("_decode_note", "Claims verified via issuer JWKs");
                return claims;
            } catch (Exception ignored) {
                // fall back to best-effort decode
            }
        }
        // Best-effort parse as JWT without validation (for display only)
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                // Parse payload JSON into claims map
                Map<String, Object> payloadClaims = mapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                claims.putAll(payloadClaims);
                claims.put("_decode_note", "Unverified parse of JWT payload");
            } else {
                claims.put("_decode_note", "Access token is not a JWT (likely opaque)");
            }
        } catch (Exception e) {
            claims.put("_decode_note", "Failed to decode access token");
            claims.put("_error", e.getMessage());
        }
        return claims;
    }


    // ---- Helpers for pretty JSON ----

    /** Pretty print arbitrary POJO/Map to JSON string. Returns "{}" on failure. */
    private String toPrettyJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /** Pretty print a raw JSON string if it parses; otherwise returns original string. */
    private String toPrettyJson(String rawJson) {
        try {
            JsonNode node = mapper.readTree(rawJson);
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return rawJson; // not valid JSON; show as-is
        }
    }
}