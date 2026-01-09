package com.reposync.monitoring.service;

import com.reposync.monitoring.config.MonitoringProperties;
import com.reposync.monitoring.model.ServiceHealth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Main monitoring service that coordinates health checks and metrics collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

  private final HealthCheckService healthCheckService;
  private final MetricsCollectionService metricsCollectionService;
  private final MonitoringProperties monitoringProperties;

  /**
   * Scheduled health check for all services.
   */
  @Scheduled(fixedDelayString = "${monitoring.scrape-interval-seconds:30}000")
  public void performScheduledHealthChecks() {
    log.info("Starting scheduled health checks");

    if (monitoringProperties.getServices() == null) {
      log.warn("No services configured for monitoring");
      return;
    }

    List<ServiceHealth> healthResults = monitoringProperties.getServices().stream()
        .filter(MonitoringProperties.ServiceEndpoint::isEnabled)
        .map(service -> healthCheckService.checkServiceHealth(service.getName(), service.getUrl()))
        .collect(Collectors.toList());

    long healthyCount = healthResults.stream().filter(ServiceHealth::isHealthy).count();
    log.info("Health check completed: {}/{} services healthy",
        healthyCount, healthResults.size());
  }

  /**
   * Scheduled metrics collection.
   */
  @Scheduled(fixedDelayString = "${monitoring.scrape-interval-seconds:30}000")
  public void performScheduledMetricsCollection() {
    if (monitoringProperties.getServices() == null) {
      return;
    }

    monitoringProperties.getServices().stream()
        .filter(MonitoringProperties.ServiceEndpoint::isEnabled)
        .forEach(service ->
            metricsCollectionService.collectServiceMetrics(service.getName(), service.getUrl())
        );
  }
}

