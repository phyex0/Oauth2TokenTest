package com.phyex.oauth2tokentest.controller;

import com.nimbusds.oauth2.sdk.TokenResponse;
import com.phyex.oauth2tokentest.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/token-controller")
public class TokenController {

    private final TokenService tokenService;


    @GetMapping("/grant-access-nimbus")
    //TODO: Try to implement SSL into token calls
    public ResponseEntity<TokenResponse> getTokenWithGrantAccess(@RequestParam String userName, @RequestParam String password) {
        TokenResponse token = tokenService.getTokenWithGrantAccess(userName, password);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/pkce-nimbus")
    //TODO: Try to implement SSL into token calls
    public ResponseEntity<TokenResponse> getTokenWithPkce(@RequestParam String userName, @RequestParam String password) {
        TokenResponse token = tokenService.getTokenWithPkce(userName, password);
        return ResponseEntity.ok(token);
    }
}
