package com.reposync.orchestrator.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

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

    /**
     * Create a connection provider with proper pooling settings to prevent premature disconnections.
     */
    private ConnectionProvider createConnectionProvider() {
        return ConnectionProvider.builder("custom-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofMinutes(10))
                .maxLifeTime(Duration.ofMinutes(30))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofMinutes(5))
                .build();
    }

    private HttpClient createHttpClient() {
        return HttpClient.create(createConnectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofMinutes(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES))
                                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.MINUTES)));
    }

    /**
     * Configure exchange strategies with increased buffer size for large payloads.
     */
    private ExchangeStrategies createExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }

    @Bean
    public WebClient githubWebClient() {
        return WebClient.builder()
                .baseUrl(githubServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient processorWebClient() {
        return WebClient.builder()
                .baseUrl(processorServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient embeddingWebClient() {
        return WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient milvusWebClient() {
        return WebClient.builder()
                .baseUrl(milvusServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }
}

