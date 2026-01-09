package com.reposync.monitoring.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service health status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {

  private String serviceName;
  private boolean healthy;
  private String status;
  private long responseTimeMs;
  private LocalDateTime timestamp;
  private Map<String, Object> details;
}

