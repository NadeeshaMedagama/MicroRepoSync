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

        log.info("=== Starting sync workflow - Job ID: {} ===", jobId);
        log.info("Organization: {}, Filter: {}, Collection: {}", organization, filterKeyword, collectionName);

        try {
            // Step 1: Fetch repositories from GitHub
            log.info("Step 1: Fetching repositories from organization: {}", organization);
            List<RepositoryInfo> repositories = null;
            try {
                repositories = fetchRepositories();
                log.info("✓ Found {} repositories", repositories.size());
            } catch (Exception e) {
                System.err.println("✗ STEP 1 FAILED - Error fetching repositories");
                System.err.println("Error: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Root Cause: " + e.getCause().getMessage());
                }
                log.error("✗ Step 1 FAILED - Error fetching repositories: {}", e.getMessage(), e);
                throw new RuntimeException("Step 1 failed: " + e.getMessage(), e);
            }

            if (repositories.isEmpty()) {
                log.info("No repositories found matching criteria - completing with SUCCESS");
                return buildResult(jobId, startTime, 0, 0, 0, 0, "SUCCESS",
                        "No repositories found matching criteria");
            }

            // Step 2: Fetch documents from all repositories
            log.info("Step 2: Fetching documents from {} repositories", repositories.size());
            List<DocumentContent> allDocuments = new ArrayList<>();
            try {
                for (RepositoryInfo repo : repositories) {
                    try {
                        String[] parts = repo.getFullName().split("/");
                        log.debug("Fetching documents from {}", repo.getFullName());
                        List<DocumentContent> docs = fetchDocuments(parts[0], parts[1]);
                        allDocuments.addAll(docs);
                        log.info("  ✓ Fetched {} documents from {}", docs.size(), repo.getFullName());
                    } catch (Exception e) {
                        log.error("  ✗ Error fetching documents from {}: {}", repo.getFullName(), e.getMessage());
                        // Continue with other repos
                    }
                }
                log.info("✓ Step 2 complete - Total documents fetched: {}", allDocuments.size());
            } catch (Exception e) {
                log.error("✗ Step 2 FAILED - Error in document fetching loop: {}", e.getMessage(), e);
                throw new RuntimeException("Step 2 failed: " + e.getMessage(), e);
            }

            if (allDocuments.isEmpty()) {
                log.info("No documents found in repositories - completing with SUCCESS");
                return buildResult(jobId, startTime, repositories.size(), 0, 0, 0, "SUCCESS",
                        "No documents found in repositories");
            }

            // Step 3: Chunk documents
            log.info("Step 3: Chunking {} documents", allDocuments.size());
            List<TextChunk> chunks = null;
            try {
                chunks = chunkDocuments(allDocuments);
                log.info("✓ Step 3 complete - Created {} chunks", chunks.size());
            } catch (Exception e) {
                System.err.println("✗ STEP 3 FAILED - Error chunking documents");
                System.err.println("Error: " + e.getMessage());
                log.error("✗ Step 3 FAILED - Error chunking documents: {}", e.getMessage(), e);
                throw new RuntimeException("Step 3 failed: " + e.getMessage(), e);
            }

            if (chunks.isEmpty()) {
                log.info("No chunks created - completing with SUCCESS");
                return buildResult(jobId, startTime, repositories.size(), allDocuments.size(),
                        0, 0, "SUCCESS", "No chunks created");
            }

            // Step 4: Generate embeddings
            log.info("Step 4: Generating embeddings for {} chunks", chunks.size());
            List<EmbeddingVector> vectors = null;
            try {
                vectors = generateEmbeddings(chunks);
                log.info("✓ Step 4 complete - Generated {} embeddings", vectors.size());
            } catch (Exception e) {
                System.err.println("✗ STEP 4 FAILED - Error generating embeddings");
                System.err.println("Error: " + e.getMessage());
                log.error("✗ Step 4 FAILED - Error generating embeddings: {}", e.getMessage(), e);
                throw new RuntimeException("Step 4 failed: " + e.getMessage(), e);
            }

            // Step 5: Ensure Milvus collection exists
            log.info("Step 5: Ensuring Milvus collection exists: {}", collectionName);
            try {
                ensureCollection();
                log.info("✓ Step 5 complete - Collection ready");
            } catch (Exception e) {
                System.err.println("✗ STEP 5 FAILED - Error ensuring Milvus collection");
                System.err.println("Error: " + e.getMessage());
                log.error("✗ Step 5 FAILED - Error ensuring collection: {}", e.getMessage(), e);
                throw new RuntimeException("Step 5 failed: " + e.getMessage(), e);
            }

            // Step 6: Upsert vectors to Milvus
            log.info("Step 6: Upserting {} vectors to Milvus", vectors.size());
            try {
                upsertVectors(vectors);
                log.info("✓ Step 6 complete - Vectors upserted");
            } catch (Exception e) {
                System.err.println("✗ STEP 6 FAILED - Error upserting vectors");
                System.err.println("Error: " + e.getMessage());
                log.error("✗ Step 6 FAILED - Error upserting vectors: {}", e.getMessage(), e);
                throw new RuntimeException("Step 6 failed: " + e.getMessage(), e);
            }

            LocalDateTime endTime = LocalDateTime.now();
            log.info("=== Sync workflow completed successfully - Job ID: {} ===", jobId);

            return buildResult(jobId, startTime, repositories.size(), allDocuments.size(),
                    chunks.size(), vectors.size(), "SUCCESS", null);

        } catch (Exception e) {
            // Print to System.err so it shows up prominently in CI logs (red/highlighted)
            System.err.println("!!!!! WORKFLOW SYNC FAILED !!!!!");
            System.err.println("Job ID: " + jobId);
            System.err.println("Error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace(System.err);

            log.error("=== Sync workflow FAILED - Job ID: {} ===", jobId);
            log.error("Error details: {}", e.getMessage(), e);
            LocalDateTime endTime = LocalDateTime.now();

            // Build detailed error message
            String errorDetails = "Workflow failed: " + e.getMessage();
            if (e.getCause() != null) {
                errorDetails += " (Cause: " + e.getCause().getMessage() + ")";
            }

            return buildResult(jobId, startTime, 0, 0, 0, 0, "FAILED", errorDetails);
        }
    }

    private List<RepositoryInfo> fetchRepositories() {
        try {
            log.debug("Calling GitHub service at: {}/api/github/repositories?organization={}&filterKeyword={}",
                    githubWebClient, organization, filterKeyword);

            List<RepositoryInfo> repos = githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/github/repositories")
                            .queryParam("organization", organization)
                            .queryParam("filterKeyword", filterKeyword)
                            .build())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("GitHub service error: " + response.statusCode() + " - " + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<RepositoryInfo>>() {})
                    .block();

            return repos != null ? repos : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch repositories from GitHub service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch repositories from GitHub service: " + e.getMessage(), e);
        }
    }

    private List<DocumentContent> fetchDocuments(String owner, String repo) {
        try {
            log.debug("Fetching documents from {}/{}", owner, repo);

            List<DocumentContent> docs = githubWebClient.get()
                    .uri("/api/github/documents/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("GitHub service error for " + owner + "/" + repo + ": " + response.statusCode() + " - " + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<DocumentContent>>() {})
                    .block();

            return docs != null ? docs : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch documents from {}/{}: {}", owner, repo, e.getMessage());
            throw new RuntimeException("Failed to fetch documents from " + owner + "/" + repo + ": " + e.getMessage(), e);
        }
    }

    private List<TextChunk> chunkDocuments(List<DocumentContent> documents) {
        try {
            log.info("Sending {} documents to processor service for chunking", documents.size());

            List<TextChunk> chunks = processorWebClient.post()
                    .uri("/api/processor/chunk/batch")
                    .bodyValue(documents)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Processor service error: " + response.statusCode() + " - " + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<TextChunk>>() {})
                    .block();

            log.info("Processor service returned {} chunks", chunks != null ? chunks.size() : 0);
            return chunks != null ? chunks : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to chunk documents via processor service: {}", e.getMessage(), e);
            System.err.println("CHUNKING FAILED: " + e.getMessage());
            throw new RuntimeException("Failed to chunk documents: " + e.getMessage(), e);
        }
    }

    private List<EmbeddingVector> generateEmbeddings(List<TextChunk> chunks) {
        try {
            log.info("Sending {} chunks to embedding service for embedding generation", chunks.size());

            List<EmbeddingVector> vectors = embeddingWebClient.post()
                    .uri("/api/embedding/generate/batch")
                    .bodyValue(chunks)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Embedding service error: " + response.statusCode() + " - " + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<EmbeddingVector>>() {})
                    .block();

            log.info("Embedding service returned {} vectors", vectors != null ? vectors.size() : 0);
            return vectors != null ? vectors : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to generate embeddings via embedding service: {}", e.getMessage(), e);
            System.err.println("EMBEDDING GENERATION FAILED: " + e.getMessage());
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }

    private void ensureCollection() {
        try {
            log.debug("Checking if collection '{}' exists", collectionName);

            Boolean exists = milvusWebClient.get()
                    .uri("/api/milvus/collection/{collectionName}/exists", collectionName)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Milvus service error checking collection: " + response.statusCode() + " - " + body)))
                    .bodyToMono(Boolean.class)
                    .block();

            if (exists == null || !exists) {
                log.info("Collection '{}' does not exist, creating it with dimension {}", collectionName, vectorDimension);
                milvusWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/milvus/collection/create")
                                .queryParam("collectionName", collectionName)
                                .queryParam("dimension", vectorDimension)
                                .build())
                        .retrieve()
                        .onStatus(status -> !status.is2xxSuccessful(),
                                response -> response.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("Milvus service error creating collection: " + response.statusCode() + " - " + body)))
                        .bodyToMono(String.class)
                        .block();
                log.info("Collection '{}' created successfully", collectionName);
            } else {
                log.debug("Collection '{}' already exists", collectionName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure Milvus collection exists: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to ensure Milvus collection exists: " + e.getMessage(), e);
        }
    }

    private void upsertVectors(List<EmbeddingVector> vectors) {
        try {
            log.debug("Upserting {} vectors to collection '{}'", vectors.size(), collectionName);

            milvusWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/milvus/vectors/upsert")
                            .queryParam("collectionName", collectionName)
                            .build())
                    .bodyValue(vectors)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Milvus service error upserting vectors: " + response.statusCode() + " - " + body)))
                    .bodyToMono(String.class)
                    .block();

            log.debug("Successfully upserted {} vectors", vectors.size());
        } catch (Exception e) {
            log.error("Failed to upsert vectors to Milvus: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vectors to Milvus: " + e.getMessage(), e);
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

