package com.reposync.monitoring.service;

import com.reposync.monitoring.model.ServiceHealth;

/**
 * Service for performing health checks.
 */
public interface HealthCheckService {

  /**
   * Check health of a service.
   *
   * @param serviceName name of the service
   * @param serviceUrl  URL of the service
   * @return health status
   */
  ServiceHealth checkServiceHealth(String serviceName, String serviceUrl);

  /**
   * Check if service is available.
   *
   * @param serviceUrl URL of the service
   * @return true if available
   */
  boolean isServiceAvailable(String serviceUrl);
}

