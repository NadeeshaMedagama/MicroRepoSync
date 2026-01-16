package com.reposync.orchestrator;

import com.reposync.orchestrator.service.WorkflowOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class OrchestratorServiceApplication {

    private final WorkflowOrchestrator workflowOrchestrator;

    @Value("${reposync.auto-sync-on-startup:true}")
    private boolean autoSyncOnStartup;

    public OrchestratorServiceApplication(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoSyncOnStartup) {
            log.info("🚀 Application is ready. Auto-triggering initial sync workflow...");
            new Thread(() -> {
                try {
                    // Small delay to ensure all services are fully initialized
                    Thread.sleep(5000);
                    workflowOrchestrator.executeSyncWorkflow();
                    log.info("✅ Initial sync workflow completed");
                } catch (Exception e) {
                    log.error("❌ Initial sync workflow failed: {}", e.getMessage(), e);
                }
            }).start();
        } else {
            log.info("ℹ️ Auto-sync on startup is disabled. Use POST /api/orchestrator/sync to trigger manually.");
        }
    }
}

