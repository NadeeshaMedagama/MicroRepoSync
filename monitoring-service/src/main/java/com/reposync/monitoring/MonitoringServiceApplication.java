package com.reposync.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Monitoring Service.
 */
@SpringBootApplication
@EnableScheduling
public class MonitoringServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MonitoringServiceApplication.class, args);
  }
}

