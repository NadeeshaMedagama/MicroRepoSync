package com.reposync.orchestrator.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

    private HttpClient createHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofSeconds(120))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient githubWebClient() {
        return WebClient.builder()
                .baseUrl(githubServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
    }

    @Bean
    public WebClient processorWebClient() {
        return WebClient.builder()
                .baseUrl(processorServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
    }

    @Bean
    public WebClient embeddingWebClient() {
        return WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
    }

    @Bean
    public WebClient milvusWebClient() {
        return WebClient.builder()
                .baseUrl(milvusServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
    }
}

