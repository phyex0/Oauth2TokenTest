package com.phyex.oauth2tokentest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("oauth")
public class OauthConfig {
    private String clientId;
    private String issureUrl;
}
