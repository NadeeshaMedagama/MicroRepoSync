package com.reposync.monitoring.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for monitoring.
 */
@Data
@Component
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

  private int scrapeIntervalSeconds = 30;
  private long healthCheckTimeoutMs = 5000;
  private List<ServiceEndpoint> services;

  /**
   * Service endpoint configuration.
   */
  @Data
  public static class ServiceEndpoint {

    private String name;
    private String url;
    private boolean enabled = true;
  }
}

