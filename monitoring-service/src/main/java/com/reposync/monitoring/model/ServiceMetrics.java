package com.reposync.monitoring.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service metrics data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMetrics {

  private String serviceName;
  private LocalDateTime timestamp;
  private Map<String, Double> metrics;
  private Map<String, Object> metadata;
}

