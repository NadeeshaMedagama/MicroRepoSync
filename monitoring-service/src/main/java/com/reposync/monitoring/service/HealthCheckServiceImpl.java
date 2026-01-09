package com.reposync.monitoring.service;

import com.reposync.monitoring.config.MonitoringProperties;
import com.reposync.monitoring.model.ServiceHealth;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementation of HealthCheckService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

  private final WebClient.Builder webClientBuilder;
  private final MeterRegistry meterRegistry;
  private final MonitoringProperties monitoringProperties;

  @Override
  public ServiceHealth checkServiceHealth(String serviceName, String serviceUrl) {
    long startTime = System.currentTimeMillis();

    try {
      WebClient webClient = webClientBuilder.baseUrl(serviceUrl).build();

      Map<String, Object> healthResponse = webClient.get()
          .uri("/actuator/health")
          .retrieve()
          .bodyToMono(Map.class)
          .timeout(java.time.Duration.ofMillis(monitoringProperties.getHealthCheckTimeoutMs()))
          .onErrorResume(e -> Mono.empty())
          .block();

      long responseTime = System.currentTimeMillis() - startTime;

      // Record metrics
      meterRegistry.gauge("service.health.response.time",
          Tags.of("service", serviceName),
          responseTime);

      String status = healthResponse != null
          ? (String) healthResponse.getOrDefault("status", "UNKNOWN") : "DOWN";

      // Record service status
      double statusValue = "UP".equalsIgnoreCase(status) ? 1.0 : 0.0;
      meterRegistry.gauge("service.health.status",
          Tags.of("service", serviceName, "status", status),
          statusValue);

      return ServiceHealth.builder()
          .serviceName(serviceName)
          .healthy("UP".equalsIgnoreCase(status))
          .status(status)
          .responseTimeMs(responseTime)
          .timestamp(LocalDateTime.now())
          .details(healthResponse)
          .build();

    } catch (Exception e) {
      long responseTime = System.currentTimeMillis() - startTime;
      log.error("Health check failed for {}: {}", serviceName, e.getMessage());

      // Record failure
      meterRegistry.counter("service.health.check.failures",
          Tags.of("service", serviceName)).increment();

      return ServiceHealth.builder()
          .serviceName(serviceName)
          .healthy(false)
          .status("ERROR")
          .responseTimeMs(responseTime)
          .timestamp(LocalDateTime.now())
          .details(Map.of("error", e.getMessage()))
          .build();
    }
  }

  @Override
  public boolean isServiceAvailable(String serviceUrl) {
    try {
      WebClient webClient = webClientBuilder.baseUrl(serviceUrl).build();
      webClient.get()
          .uri("/actuator/health")
          .retrieve()
          .toBodilessEntity()
          .timeout(java.time.Duration.ofMillis(5000))
          .block();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}

