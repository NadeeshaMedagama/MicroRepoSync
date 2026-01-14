package com.reposync.orchestrator.service;

import com.reposync.common.dto.*;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WorkflowOrchestrator {

    private final WebClient githubWebClient;
    private final WebClient processorWebClient;
    private final WebClient embeddingWebClient;
    private final WebClient milvusWebClient;

    @Value("${reposync.organization}")
    private String organization;

    @Value("${reposync.filter-keyword:}")
    private String filterKeyword;

    @Value("${milvus.collection-name}")
    private String collectionName;

    @Value("${azure.openai.vector-dimension:1536}")
    private int vectorDimension;

    public WorkflowOrchestrator(
            @Qualifier("githubWebClient") WebClient githubWebClient,
            @Qualifier("processorWebClient") WebClient processorWebClient,
            @Qualifier("embeddingWebClient") WebClient embeddingWebClient,
            @Qualifier("milvusWebClient") WebClient milvusWebClient) {
        this.githubWebClient = githubWebClient;
        this.processorWebClient = processorWebClient;
        this.embeddingWebClient = embeddingWebClient;
        this.milvusWebClient = milvusWebClient;
    }

    @Scheduled(cron = "${reposync.schedule.cron:0 0 8 * * *}")
    public void scheduledSync() {
        log.info("Scheduled sync job starting at {}", LocalDateTime.now());
        executeSyncWorkflow();
    }

    @Retry(name = "syncWorkflow", fallbackMethod = "syncWorkflowFallback")
    public SyncJobResult executeSyncWorkflow() {
        String jobId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        log.info("Starting sync workflow - Job ID: {}", jobId);

        try {
            // Step 1: Fetch repositories from GitHub
            log.info("Step 1: Fetching repositories from organization: {}", organization);
            List<RepositoryInfo> repositories = fetchRepositories();
            log.info("Found {} repositories", repositories.size());

            if (repositories.isEmpty()) {
                return buildResult(jobId, startTime, 0, 0, 0, 0, "SUCCESS",
                        "No repositories found matching criteria");
            }

            // Step 2: Fetch documents from all repositories
            log.info("Step 2: Fetching documents from repositories");
            List<DocumentContent> allDocuments = new ArrayList<>();
            for (RepositoryInfo repo : repositories) {
                try {
                    String[] parts = repo.getFullName().split("/");
                    List<DocumentContent> docs = fetchDocuments(parts[0], parts[1]);
                    allDocuments.addAll(docs);
                    log.info("Fetched {} documents from {}", docs.size(), repo.getFullName());
                } catch (Exception e) {
                    log.error("Error fetching documents from {}: {}", repo.getFullName(), e.getMessage());
                }
            }
            log.info("Total documents fetched: {}", allDocuments.size());

            if (allDocuments.isEmpty()) {
                return buildResult(jobId, startTime, repositories.size(), 0, 0, 0, "SUCCESS",
                        "No documents found in repositories");
            }

            // Step 3: Chunk documents
            log.info("Step 3: Chunking {} documents", allDocuments.size());
            List<TextChunk> chunks = chunkDocuments(allDocuments);
            log.info("Created {} chunks", chunks.size());

            if (chunks.isEmpty()) {
                return buildResult(jobId, startTime, repositories.size(), allDocuments.size(),
                        0, 0, "SUCCESS", "No chunks created");
            }

            // Step 4: Generate embeddings
            log.info("Step 4: Generating embeddings for {} chunks", chunks.size());
            List<EmbeddingVector> vectors = generateEmbeddings(chunks);
            log.info("Generated {} embeddings", vectors.size());

            // Step 5: Ensure Milvus collection exists
            log.info("Step 5: Ensuring Milvus collection exists: {}", collectionName);
            ensureCollection();

            // Step 6: Upsert vectors to Milvus
            log.info("Step 6: Upserting {} vectors to Milvus", vectors.size());
            upsertVectors(vectors);

            LocalDateTime endTime = LocalDateTime.now();
            log.info("Sync workflow completed successfully - Job ID: {}", jobId);

            return buildResult(jobId, startTime, repositories.size(), allDocuments.size(),
                    chunks.size(), vectors.size(), "SUCCESS", null);

        } catch (Exception e) {
            log.error("Sync workflow failed - Job ID: {}: {}", jobId, e.getMessage(), e);
            LocalDateTime endTime = LocalDateTime.now();
            return buildResult(jobId, startTime, 0, 0, 0, 0, "FAILED", e.getMessage());
        }
    }

    private List<RepositoryInfo> fetchRepositories() {
        try {
            return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/github/repositories")
                            .queryParam("organization", organization)
                            .queryParam("filterKeyword", filterKeyword)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<RepositoryInfo>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch repositories: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch repositories from GitHub service", e);
        }
    }

    private List<DocumentContent> fetchDocuments(String owner, String repo) {
        try {
            return githubWebClient.get()
                    .uri("/api/github/documents/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DocumentContent>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch documents from {}/{}: {}", owner, repo, e.getMessage());
            throw new RuntimeException("Failed to fetch documents from " + owner + "/" + repo, e);
        }
    }

    private List<TextChunk> chunkDocuments(List<DocumentContent> documents) {
        try {
            return processorWebClient.post()
                    .uri("/api/processor/chunk/batch")
                    .bodyValue(documents)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<TextChunk>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to chunk documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to chunk documents", e);
        }
    }

    private List<EmbeddingVector> generateEmbeddings(List<TextChunk> chunks) {
        try {
            return embeddingWebClient.post()
                    .uri("/api/embedding/generate/batch")
                    .bodyValue(chunks)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EmbeddingVector>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to generate embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }

    private void ensureCollection() {
        try {
            Boolean exists = milvusWebClient.get()
                    .uri("/api/milvus/collection/{collectionName}/exists", collectionName)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (exists == null || !exists) {
                milvusWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/milvus/collection/create")
                                .queryParam("collectionName", collectionName)
                                .queryParam("dimension", vectorDimension)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }
        } catch (Exception e) {
            log.error("Failed to ensure collection exists: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to ensure Milvus collection exists", e);
        }
    }

    private void upsertVectors(List<EmbeddingVector> vectors) {
        try {
            milvusWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/milvus/vectors/upsert")
                            .queryParam("collectionName", collectionName)
                            .build())
                    .bodyValue(vectors)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to upsert vectors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vectors to Milvus", e);
        }
    }

    private SyncJobResult buildResult(String jobId, LocalDateTime startTime,
                                       int reposProcessed, int docsProcessed,
                                       int chunksCreated, int vectorsStored,
                                       String status, String errorMessage) {
        return SyncJobResult.builder()
                .jobId(jobId)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .repositoriesProcessed(reposProcessed)
                .documentsProcessed(docsProcessed)
                .chunksCreated(chunksCreated)
                .vectorsStored(vectorsStored)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    public SyncJobResult syncWorkflowFallback(Exception e) {
        log.error("Sync workflow fallback triggered due to: {}", e.getMessage());
        return SyncJobResult.builder()
                .jobId(UUID.randomUUID().toString())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .repositoriesProcessed(0)
                .documentsProcessed(0)
                .chunksCreated(0)
                .vectorsStored(0)
                .status("FAILED")
                .errorMessage("Workflow failed after retries: " + e.getMessage())
                .build();
    }
}

