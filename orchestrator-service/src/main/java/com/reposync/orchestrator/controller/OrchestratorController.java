package com.reposync.orchestrator.controller;

import com.reposync.common.dto.SyncJobResult;
import com.reposync.orchestrator.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {

    private final WorkflowOrchestrator workflowOrchestrator;

    @PostMapping("/sync")
    public ResponseEntity<SyncJobResult> triggerSync() {
        log.info("Manual sync triggered via API");
        SyncJobResult result = workflowOrchestrator.executeSyncWorkflow();

        // Return HTTP 500 if sync failed to make it clear in CI/CD
        if ("FAILED".equals(result.getStatus())) {
            log.error("Sync workflow failed: {}", result.getErrorMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Orchestrator Service is running");
    }
}

