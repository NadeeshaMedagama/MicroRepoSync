package com.reposync.orchestrator.controller;

import com.reposync.common.dto.SyncJobResult;
import com.reposync.orchestrator.service.WorkflowOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final WorkflowOrchestrator workflowOrchestrator;
    private final WebClient githubWebClient;
    private final WebClient processorWebClient;
    private final WebClient embeddingWebClient;
    private final WebClient milvusWebClient;

    @Value("${milvus.collection-name}")
    private String collectionName;

    public OrchestratorController(
            WorkflowOrchestrator workflowOrchestrator,
            @Qualifier("githubWebClient") WebClient githubWebClient,
            @Qualifier("processorWebClient") WebClient processorWebClient,
            @Qualifier("embeddingWebClient") WebClient embeddingWebClient,
            @Qualifier("milvusWebClient") WebClient milvusWebClient) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.githubWebClient = githubWebClient;
        this.processorWebClient = processorWebClient;
        this.embeddingWebClient = embeddingWebClient;
        this.milvusWebClient = milvusWebClient;
    }

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

    @GetMapping("/preflight")
    public ResponseEntity<Map<String, Object>> preflightCheck() {
        log.info("Running preflight connectivity check...");
        Map<String, Object> result = new HashMap<>();
        boolean allHealthy = true;

        // Check GitHub service
        try {
            String githubHealth = githubWebClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            result.put("github-service", "OK");
            log.info("✅ GitHub service: OK");
        } catch (Exception e) {
            result.put("github-service", "FAILED: " + e.getMessage());
            log.error("❌ GitHub service: FAILED - {}", e.getMessage());
            allHealthy = false;
        }

        // Check Processor service
        try {
            String processorHealth = processorWebClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            result.put("processor-service", "OK");
            log.info("✅ Processor service: OK");
        } catch (Exception e) {
            result.put("processor-service", "FAILED: " + e.getMessage());
            log.error("❌ Processor service: FAILED - {}", e.getMessage());
            allHealthy = false;
        }

        // Check Embedding service
        try {
            String embeddingHealth = embeddingWebClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            result.put("embedding-service", "OK");
            log.info("✅ Embedding service: OK");
        } catch (Exception e) {
            result.put("embedding-service", "FAILED: " + e.getMessage());
            log.error("❌ Embedding service: FAILED - {}", e.getMessage());
            allHealthy = false;
        }

        // Check Milvus service (and indirectly Zilliz Cloud connection)
        try {
            Boolean milvusExists = milvusWebClient.get()
                    .uri("/api/milvus/collection/{collectionName}/exists", collectionName)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            result.put("milvus-service", "OK (collection exists: " + milvusExists + ")");
            log.info("✅ Milvus service: OK (collection exists: {})", milvusExists);
        } catch (Exception e) {
            result.put("milvus-service", "FAILED: " + e.getMessage());
            log.error("❌ Milvus service: FAILED - {}", e.getMessage());
            allHealthy = false;
        }

        result.put("overall", allHealthy ? "READY" : "NOT_READY");

        if (allHealthy) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(503).body(result);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Orchestrator Service is running");
    }
}
