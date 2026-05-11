package com.phyex.oauth2tokentest.service;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.phyex.oauth2tokentest.config.OauthConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final HttpClient httpClient;
    private final OauthConfig oauthConfig;

    @Override
    @SneakyThrows
    public TokenResponse getTokenWithGrantAccess(String userName, String password) {
        // 1. Resolve the "Well-Known" URL to get endpoints
        // Note: Use the base Issuer URL (e.g., https://auth.example.com/realm)
        // The SDK automatically appends /.well-known/openid-configuration
        Issuer issuer = new Issuer(oauthConfig.getIssuerUrl());
        OIDCProviderMetadata providerMetadata = OIDCProviderMetadata.resolve(issuer);

        URI tokenEndpoint = providerMetadata.getTokenEndpointURI();

        // 2. Set up the Client ID and User Credentials
        ClientID clientID = new ClientID(oauthConfig.getClientId());

        // 3. Create the Password Grant
        AuthorizationGrant passwordGrant = new ResourceOwnerPasswordCredentialsGrant(userName, new Secret(password));

        // 4. Add Scope
        Scope scope = new Scope(oauthConfig.getScope());

        // 5. Construct and send the Token Request
        TokenRequest request = new TokenRequest(tokenEndpoint, clientID, passwordGrant, scope);

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        // 6. Handle the result
        if (response.indicatesSuccess()) {
            AccessTokenResponse successResponse = response.toSuccessResponse();

            // Get your tokens!
            String accessToken = successResponse.getTokens().getAccessToken().getValue();
            String refreshToken = null;
            if (successResponse.getTokens().getRefreshToken() != null) {
                refreshToken = successResponse.getTokens().getRefreshToken().getValue();
            }

            System.out.println("Access Token: " + accessToken);
            System.out.println("Refresh Token: " + refreshToken);
        } else {
            // Handle errors (like invalid credentials or grant type disabled)
            TokenErrorResponse errorResponse = response.toErrorResponse();
            System.err.println("Error: " + errorResponse.getErrorObject().getDescription());
        }

        return response;
    }


    @Override
    @SneakyThrows
    public TokenResponse getTokenWithPkce(String userName, String password) {
        CodeVerifier codeVerifier = new CodeVerifier();
        CodeChallenge codeChallenge = CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier);
        String auth = getAuth(codeChallenge);

        //1- Authorize
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(auth)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String cookieHeader = response.headers().allValues("Set-Cookie").stream()
                .map(cookie -> cookie.split(";")[0]) // Capture "KEY=VALUE"
                .collect(Collectors.joining("; "));
        //2- Get Login Challange then Get AuthCode
        String authCode = getAuthCode(response.body(), userName, password, cookieHeader);

        //3- Get Token
        TokenResponse tokenWithNimbus = getTokenWithNimbus(authCode, codeVerifier);


        return tokenWithNimbus;
    }

    static final String AUTH = "http://localhost:8090/realms/spring-oauth-test-realm/protocol/openid-connect/auth";
    static final String TOKEN = "http://localhost:8090/realms/spring-oauth-test-realm/protocol/openid-connect/token";
    static final String callbackUrl = "http://localhost:8081/callback";

    private String getAuth(CodeChallenge codeChallenge) {
        return new StringBuilder()
                .append(AUTH)
                .append("?client_id=").append(oauthConfig.getClientId2())
                .append("&response_type=code")
                .append("&scope=").append(oauthConfig.getScope()[0])
                .append("&code_challenge=").append(codeChallenge)
                .append("&code_challenge_method=S256")
                .append("&redirect_uri=").append(callbackUrl)
                .toString();
    }

    @SneakyThrows
    private String getAuthCode(String authResponse, String userName, String password, String authCookie) {
        // --- 1. Extract the Action URL ---
        String html = authResponse;
        // A simple way to grab the URL between action=" and "
        String loginActionUrl = html.split("id=\"kc-form-login\"")[1]
                .split("action=\"")[1]
                .split("\"")[0]
                .replace("&amp;", "&"); // Clean up HTML entities

        // --- 2. Build the Login POST Request ---
        // Keycloak expects these specific form field names
        String formBody = Stream.of(
                        new AbstractMap.SimpleEntry<>("username", userName),
                        new AbstractMap.SimpleEntry<>("password", password),
                        new AbstractMap.SimpleEntry<>("credentialId", "") // Found in your HTML as a hidden input
                )
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginActionUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Cookie", authCookie)
                // Use the base AUTH URL as the referer
                .header("Referer", AUTH)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        // --- 3. Extract Auth Code from Redirect ---
        // If successful, Keycloak returns a 302 redirect
        String redirectLocation = loginResponse.headers()
                .firstValue("Location")
                .orElseThrow(() -> new RuntimeException("Login failed. Check credentials or Keycloak logs."));

        // Example: http://localhost:8081/callback?code=76f9...
        String authCode = redirectLocation.split("code=")[1].split("&")[0];
        return authCode;
    }

    private TokenResponse getTokenWithNimbus(String code, CodeVerifier codeVerifier) {
        try {
            // 1. Prepare the credentials and endpoints
            ClientID clientID = new ClientID(oauthConfig.getClientId2());
            URI tokenEndPoint = URI.create(TOKEN);
            URI callbackURI = URI.create(callbackUrl);

            // 2. Create the Grant (This is where the PKCE "Proof" happens)
            AuthorizationCode authorizationCode = new AuthorizationCode(code);
            AuthorizationGrant codeGrant = new AuthorizationCodeGrant(
                    authorizationCode,
                    callbackURI,
                    codeVerifier
            );

            // 3. Assemble the request
            TokenRequest tokenRequest = new TokenRequest(tokenEndPoint, clientID, codeGrant);

            // 4. Send and Parse
            // Note: .send() is a blocking call. Ensure this is inside your
            // listener logic before switching scenes.
            TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

            if (!tokenResponse.indicatesSuccess()) {
                System.err.println("Token Request Failed: " +
                        tokenResponse.toErrorResponse().getErrorObject().getDescription());
            }

            return tokenResponse;

        } catch (Exception e) {
            System.err.println("Error during Token Exchange: " + e.getMessage());
            return null;
        }
    }

}
