package com.phyex.oauth2tokentest.service;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.phyex.oauth2tokentest.config.OauthConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

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
    //TODO not valid not usable needs browser redirect
    public TokenResponse getTokenWithPkce(String userName, String password) {
        URI authEndpoint = new URI(oauthConfig.getIssuerUrl() + "/protocol/openid-connect/auth");
        URI tokenEndpoint = new URI(oauthConfig.getIssuerUrl() +"/protocol/openid-connect/token");
        ClientID clientID = new ClientID(oauthConfig.getClientId2());
        URI callback = new URI("http://localhost:8090/callback");

        // --- STEP A: GENERATE PKCE ---
        // Create a random verifier (the secret)
        CodeVerifier verifier = new CodeVerifier();

        // Create the challenge (the hash) to send to Keycloak
        CodeChallenge challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);

        // 2. Build Authorization Request (Send to User's Browser)
        State state = new State();
        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                new Scope(oauthConfig.getScope()),
                clientID,
                callback)
                .endpointURI(authEndpoint)
                .state(state)
                .codeChallenge(challenge, CodeChallengeMethod.S256) // PKCE added here
                .build();

        System.out.println("Redirect user to: " + authRequest.toURI());

        // --- STEP B: TOKEN EXCHANGE (After user returns with ?code=...) ---

        // Mocking the received code from the callback
        AuthorizationCode code = new AuthorizationCode("RECEIVED_CODE_FROM_URL");


        TokenRequest tokenRequest = new TokenRequest(
                tokenEndpoint,
                new ClientSecretBasic(clientID, new Secret("")), // Public clients usually have empty secret
                new AuthorizationCodeGrant(code, callback, verifier) // Pass verifier here!
        );

        TokenResponse response = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

        if (response.indicatesSuccess()) {
            AccessTokenResponse successResponse = response.toSuccessResponse();
            System.out.println("Access Token: " + successResponse.getTokens().getAccessToken());
        } else {
            System.out.println("Error: " + response.toErrorResponse().getErrorObject().getDescription());
        }

        return response;
    }
}
