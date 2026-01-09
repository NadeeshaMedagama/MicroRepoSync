package com.reposync.monitoring.service;

import com.reposync.monitoring.model.ServiceMetrics;

/**
 * Service for collecting metrics from services.
 */
public interface MetricsCollectionService {

  /**
   * Collect metrics from a service.
   *
   * @param serviceName name of the service
   * @param serviceUrl  URL of the service
   * @return collected metrics
   */
  ServiceMetrics collectServiceMetrics(String serviceName, String serviceUrl);
}

