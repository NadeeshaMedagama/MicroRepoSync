package com.reposync.monitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for metrics and monitoring.
 */
@Configuration
public class MetricsConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }
}

