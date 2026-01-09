package com.reposync.monitoring.service;

import com.reposync.monitoring.model.ServiceMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementation of MetricsCollectionService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectionServiceImpl implements MetricsCollectionService {

  private final WebClient.Builder webClientBuilder;
  private final MeterRegistry meterRegistry;

  private static final List<String> IMPORTANT_METRICS = Arrays.asList(
      "jvm.memory.used",
      "jvm.memory.max",
      "system.cpu.usage",
      "http.server.requests"
  );

  @Override
  public ServiceMetrics collectServiceMetrics(String serviceName, String serviceUrl) {
    Map<String, Double> collectedMetrics = new HashMap<>();

    try {
      WebClient webClient = webClientBuilder.baseUrl(serviceUrl).build();

      // Collect important metrics
      for (String metricName : IMPORTANT_METRICS) {
        try {
          Double value = fetchMetricValue(webClient, serviceUrl, metricName);
          if (value != null) {
            collectedMetrics.put(metricName, value);

            // Register in MeterRegistry
            meterRegistry.gauge(
                "reposync." + serviceName.toLowerCase() + "." + metricName.replace(".", "_"),
                Tags.of("service", serviceName),
                value
            );
          }
        } catch (Exception e) {
          log.warn("Failed to collect metric {} from {}: {}",
              metricName, serviceName, e.getMessage());
        }
      }

      // Record successful collection
      meterRegistry.counter("metrics.collection.success",
          Tags.of("service", serviceName)).increment();

      return ServiceMetrics.builder()
          .serviceName(serviceName)
          .timestamp(LocalDateTime.now())
          .metrics(collectedMetrics)
          .metadata(Map.of("metricsCount", collectedMetrics.size()))
          .build();

    } catch (Exception e) {
      log.error("Metrics collection failed for {}: {}", serviceName, e.getMessage());

      // Record failure
      meterRegistry.counter("metrics.collection.failure",
          Tags.of("service", serviceName)).increment();

      return ServiceMetrics.builder()
          .serviceName(serviceName)
          .timestamp(LocalDateTime.now())
          .metrics(collectedMetrics)
          .metadata(Map.of("error", e.getMessage()))
          .build();
    }
  }

  private Double fetchMetricValue(WebClient webClient, String serviceUrl, String metricName) {
    // Simplified - in real implementation, parse Prometheus format
    return 0.0;
  }
}

