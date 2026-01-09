package com.reposync.monitoring.controller;

import com.reposync.monitoring.config.MonitoringProperties;
import com.reposync.monitoring.model.ServiceHealth;
import com.reposync.monitoring.service.HealthCheckService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for monitoring endpoints.
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

  private final HealthCheckService healthCheckService;
  private final MonitoringProperties monitoringProperties;

  /**
   * Get overall system health.
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getSystemHealth() {
    List<ServiceHealth> healthResults = performHealthChecks();

    long healthyCount = healthResults.stream().filter(ServiceHealth::isHealthy).count();
    boolean allHealthy = healthyCount == healthResults.size();

    return ResponseEntity.ok(Map.of(
        "status", allHealthy ? "UP" : "DEGRADED",
        "servicesHealthy", healthyCount,
        "servicesTotal", healthResults.size(),
        "services", healthResults
    ));
  }

  /**
   * Get health status of all services.
   */
  @GetMapping("/services/health")
  public ResponseEntity<List<ServiceHealth>> getAllServicesHealth() {
    return ResponseEntity.ok(performHealthChecks());
  }

  /**
   * Get health of specific service.
   */
  @GetMapping("/services/{serviceName}/health")
  public ResponseEntity<ServiceHealth> getServiceHealth(@PathVariable String serviceName) {
    return monitoringProperties.getServices().stream()
        .filter(s -> s.getName().equalsIgnoreCase(serviceName))
        .findFirst()
        .map(service -> ResponseEntity.ok(
            healthCheckService.checkServiceHealth(service.getName(), service.getUrl())))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Get list of unhealthy services.
   */
  @GetMapping("/services/unhealthy")
  public ResponseEntity<List<ServiceHealth>> getUnhealthyServices() {
    List<ServiceHealth> unhealthy = performHealthChecks().stream()
        .filter(h -> !h.isHealthy())
        .collect(Collectors.toList());

    return ResponseEntity.ok(unhealthy);
  }

  /**
   * Trigger manual health check.
   */
  @PostMapping("/health/check")
  public ResponseEntity<Map<String, Object>> triggerHealthCheck() {
    List<ServiceHealth> healthResults = performHealthChecks();

    return ResponseEntity.ok(Map.of(
        "message", "Health check completed",
        "results", healthResults
    ));
  }

  private List<ServiceHealth> performHealthChecks() {
    if (monitoringProperties.getServices() == null) {
      return List.of();
    }

    return monitoringProperties.getServices().stream()
        .filter(MonitoringProperties.ServiceEndpoint::isEnabled)
        .map(service -> healthCheckService.checkServiceHealth(service.getName(), service.getUrl()))
        .collect(Collectors.toList());
  }
}

