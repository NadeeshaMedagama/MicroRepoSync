package com.reposync.github.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubConfig {

    @Value("${github.api.base-url:https://api.github.com}")
    private String githubApiBaseUrl;

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public WebClient gitHubWebClient() {
        return WebClient.builder()
                .baseUrl(githubApiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
    }
}

