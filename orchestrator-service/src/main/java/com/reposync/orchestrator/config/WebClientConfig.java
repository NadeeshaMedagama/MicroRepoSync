package com.reposync.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.github.url}")
    private String githubServiceUrl;

    @Value("${services.processor.url}")
    private String processorServiceUrl;

    @Value("${services.embedding.url}")
    private String embeddingServiceUrl;

    @Value("${services.milvus.url}")
    private String milvusServiceUrl;

    @Bean
    public WebClient githubWebClient() {
        return WebClient.builder()
                .baseUrl(githubServiceUrl)
                .build();
    }

    @Bean
    public WebClient processorWebClient() {
        return WebClient.builder()
                .baseUrl(processorServiceUrl)
                .build();
    }

    @Bean
    public WebClient embeddingWebClient() {
        return WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .build();
    }

    @Bean
    public WebClient milvusWebClient() {
        return WebClient.builder()
                .baseUrl(milvusServiceUrl)
                .build();
    }
}

