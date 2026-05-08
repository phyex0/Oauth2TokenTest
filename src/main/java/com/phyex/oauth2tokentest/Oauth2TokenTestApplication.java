package com.phyex.oauth2tokentest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class Oauth2TokenTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth2TokenTestApplication.class, args);
    }

}
