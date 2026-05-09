package com.phyex.oauth2tokentest.service;

import com.nimbusds.oauth2.sdk.TokenResponse;

public interface TokenService {

    TokenResponse getTokenWithGrantAccess(String userName, String password);

    TokenResponse getTokenWithPkce(String userName, String password);
}
