package com.reposync.milvus.service;

import com.google.gson.JsonObject;
import com.reposync.common.dto.EmbeddingVector;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusService {

    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collection-name}")
    private String defaultCollectionName;

    private static final String ID_FIELD = "id";
    private static final String VECTOR_FIELD = "vector";
    private static final String METADATA_FIELD = "metadata";

    public void createCollection(String collectionName, int dimension) {
        try {
            log.info("=== Starting collection creation: {} with dimension {} ===", collectionName, dimension);

            // First, test the connection by listing collections
            try {
                R<io.milvus.grpc.ShowCollectionsResponse> listResponse = milvusClient.showCollections(
                        ShowCollectionsParam.newBuilder().build());
                log.info("Milvus connection test - Status: {}, Collections count: {}",
                        listResponse.getStatus(),
                        listResponse.getData() != null ? listResponse.getData().getCollectionNamesCount() : 0);
                if (listResponse.getStatus() != R.Status.Success.getCode()) {
                    String errorMessage = getResponseMessage(listResponse);
                    log.error("Milvus connection failed: {}", errorMessage);
                    throw new RuntimeException("Cannot connect to Milvus: " + errorMessage);
                }
            } catch (Exception connEx) {
                log.error("Milvus connection test failed: {}", connEx.getMessage(), connEx);
                throw new RuntimeException("Milvus connection test failed: " + connEx.getMessage(), connEx);
            }

            // Check if collection already exists
            if (hasCollection(collectionName)) {
                log.info("Collection {} already exists, will use existing collection", collectionName);
                return;
            }

            log.info("Creating new collection {} with dimension {}", collectionName, dimension);

            // Define collection schema
            FieldType idField = FieldType.newBuilder()
                    .withName(ID_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(512)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName(VECTOR_FIELD)
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            // Use JSON for metadata - well supported on Zilliz Cloud
            FieldType metadataField = FieldType.newBuilder()
                    .withName(METADATA_FIELD)
                    .withDataType(DataType.JSON)
                    .build();

            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("RepoSync collection for storing document embeddings")
                    .addFieldType(idField)
                    .addFieldType(vectorField)
                    .addFieldType(metadataField)
                    .build();

            log.debug("Sending createCollection request to Milvus...");
            R<RpcStatus> response = milvusClient.createCollection(createCollectionParam);

            // Safely get the message - the SDK's getMessage() can throw NPE when exception is null
            String responseMessage = getResponseMessage(response);
            String exceptionMessage = response.getException() != null ? response.getException().getMessage() : "none";

            log.info("Milvus createCollection response - Status: {}, Message: {}, Exception: {}",
                    response.getStatus(),
                    responseMessage,
                    exceptionMessage);

            if (response.getStatus() != R.Status.Success.getCode()) {
                String errorMsg = String.format("Milvus createCollection failed with status %d: %s (Exception: %s)",
                        response.getStatus(),
                        responseMessage,
                        exceptionMessage);
                log.error(errorMsg);
                if (response.getException() != null) {
                    log.error("Milvus exception details:", response.getException());
                }
                throw new RuntimeException(errorMsg);
            }

            log.info("Collection {} created successfully", collectionName);

            // Create index for vector field (required before loading on some Milvus versions)
            createIndex(collectionName, dimension);

            // Load collection into memory (may be automatic on Zilliz Cloud Serverless)
            loadCollection(collectionName);

        } catch (Exception e) {
            log.error("Error creating collection {}: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Failed to create collection: " + e.getMessage(), e);
        }
    }

    private void createIndex(String collectionName, int dimension) {
        log.info("Creating index for collection {} on field {}", collectionName, VECTOR_FIELD);

        // Use AUTOINDEX for Zilliz Cloud compatibility
        io.milvus.param.index.CreateIndexParam indexParam = io.milvus.param.index.CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(VECTOR_FIELD)
                .withIndexName("vector_idx")
                .withIndexType(io.milvus.param.IndexType.AUTOINDEX)
                .withMetricType(io.milvus.param.MetricType.COSINE)
                .withSyncMode(Boolean.TRUE)  // Wait for index to be ready
                .withSyncWaitingTimeout(120L)  // Wait up to 2 minutes
                .withSyncWaitingInterval(500L)  // Check every 500ms
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Creating index attempt {}/{}", attempt, maxRetries);
                R<RpcStatus> response = milvusClient.createIndex(indexParam);

                String responseMessage = getResponseMessage(response);
                log.info("Milvus createIndex response - Status: {}, Message: {}",
                        response.getStatus(), responseMessage);

                if (response.getStatus() == R.Status.Success.getCode()) {
                    log.info("✅ Index created successfully for collection {}", collectionName);
                    return;
                } else if (responseMessage.contains("index already exist") ||
                           responseMessage.contains("already exists")) {
                    log.info("Index already exists for collection {}", collectionName);
                    return;
                } else {
                    log.warn("Index creation returned non-success status: {} - {}",
                            response.getStatus(), responseMessage);
                }
            } catch (Exception e) {
                log.warn("Index creation attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("already exist")) {
                    log.info("Index already exists for collection {}", collectionName);
                    return;
                }
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(2000 * attempt);  // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // On Zilliz Cloud Serverless, index might still be created automatically
        // Log a warning but don't fail - the load operation will fail if index is truly missing
        log.warn("Index creation may have failed for {} - will attempt to load anyway", collectionName);
    }

    private void loadCollection(String collectionName) {
        log.info("Loading collection {} into memory", collectionName);

        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSyncLoad(Boolean.TRUE)  // Wait for load to complete
                .withSyncLoadWaitingTimeout(120L)  // Wait up to 2 minutes
                .withSyncLoadWaitingInterval(500L)  // Check every 500ms
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Loading collection attempt {}/{}", attempt, maxRetries);
                R<RpcStatus> response = milvusClient.loadCollection(loadParam);

                String responseMessage = getResponseMessage(response);
                log.info("Milvus loadCollection response - Status: {}, Message: {}",
                        response.getStatus(), responseMessage);

                if (response.getStatus() == R.Status.Success.getCode()) {
                    log.info("✅ Collection {} loaded into memory", collectionName);
                    return;
                } else if (responseMessage.contains("already loaded") ||
                           responseMessage.contains("load state: Loaded")) {
                    log.info("Collection {} is already loaded", collectionName);
                    return;
                } else if (responseMessage.contains("index not found")) {
                    log.warn("Index not found, waiting for index to be ready...");
                    // Wait and retry - index might still be building
                } else {
                    log.warn("Load collection returned non-success: {} - {}",
                            response.getStatus(), responseMessage);
                }
            } catch (Exception e) {
                log.warn("Load collection attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("already loaded")) {
                    log.info("Collection {} is already loaded", collectionName);
                    return;
                }
            }

            if (attempt < maxRetries) {
                try {
                    long waitTime = 5000L * attempt;  // Wait 5s, 10s, 15s
                    log.info("Waiting {}ms before retry...", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // On Zilliz Cloud Serverless, collections might be auto-loaded on first query
        log.warn("Load collection may have failed for {} - Zilliz Cloud may auto-load on first use", collectionName);
    }

    private static final int UPSERT_BATCH_SIZE = 50;  // Process vectors in smaller batches
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public void upsertVectors(String collectionName, List<EmbeddingVector> vectors) {
        try {
            if (vectors == null || vectors.isEmpty()) {
                log.warn("No vectors to upsert");
                return;
            }

            // Filter out any vectors with null IDs or vectors
            List<EmbeddingVector> validVectors = vectors.stream()
                    .filter(v -> v != null && v.getId() != null && v.getVector() != null && !v.getVector().isEmpty())
                    .collect(Collectors.toList());

            if (validVectors.isEmpty()) {
                log.warn("No valid vectors to upsert after filtering");
                return;
            }

            log.info("Upserting {} valid vectors (filtered from {} total) to collection {}",
                    validVectors.size(), vectors.size(), collectionName);

            // Log sample vector info for debugging
            if (!validVectors.isEmpty()) {
                EmbeddingVector sample = validVectors.get(0);
                log.info("Sample vector - ID: {}, Vector dimension: {}, Metadata keys: {}",
                        sample.getId(),
                        sample.getVector() != null ? sample.getVector().size() : 0,
                        sample.getMetadata() != null ? sample.getMetadata().keySet() : "null");
            }

            // Ensure collection exists with retry
            ensureCollectionExistsWithRetry(collectionName, validVectors.get(0).getVector().size());

            // Process vectors in batches for better reliability
            int totalBatches = (validVectors.size() + UPSERT_BATCH_SIZE - 1) / UPSERT_BATCH_SIZE;
            int successCount = 0;

            for (int i = 0; i < validVectors.size(); i += UPSERT_BATCH_SIZE) {
                int endIndex = Math.min(i + UPSERT_BATCH_SIZE, validVectors.size());
                List<EmbeddingVector> batch = validVectors.subList(i, endIndex);
                int batchNum = (i / UPSERT_BATCH_SIZE) + 1;

                log.info("Processing batch {}/{}: {} vectors", batchNum, totalBatches, batch.size());

                boolean batchSuccess = upsertBatchWithRetry(collectionName, batch, batchNum, totalBatches);
                if (batchSuccess) {
                    successCount += batch.size();
                }

                // Small delay between batches to avoid overwhelming Zilliz Cloud
                if (endIndex < validVectors.size()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            log.info("Successfully upserted {}/{} vectors to collection {}",
                    successCount, validVectors.size(), collectionName);

        } catch (Exception e) {
            log.error("Error upserting vectors to {}: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vectors: " + e.getMessage(), e);
        }
    }

    private void ensureCollectionExistsWithRetry(String collectionName, int dimension) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (!hasCollection(collectionName)) {
                    createCollection(collectionName, dimension);
                }
                return;
            } catch (Exception e) {
                log.warn("Attempt {}/{} to ensure collection exists failed: {}",
                        attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private boolean upsertBatchWithRetry(String collectionName, List<EmbeddingVector> batch,
                                          int batchNum, int totalBatches) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Prepare data for insertion
                List<String> ids = batch.stream()
                        .map(EmbeddingVector::getId)
                        .collect(Collectors.toList());

                List<List<Float>> vectorList = batch.stream()
                        .map(EmbeddingVector::getVector)
                        .collect(Collectors.toList());

                // Convert metadata to JsonObject for Milvus JSON field type
                List<JsonObject> jsonMetadata = batch.stream()
                        .map(v -> mapToJsonObject(v.getMetadata()))
                        .collect(Collectors.toList());

                List<InsertParam.Field> fields = new ArrayList<>();
                fields.add(new InsertParam.Field(ID_FIELD, ids));
                fields.add(new InsertParam.Field(VECTOR_FIELD, vectorList));
                fields.add(new InsertParam.Field(METADATA_FIELD, jsonMetadata));

                InsertParam insertParam = InsertParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFields(fields)
                        .build();

                log.debug("Calling Milvus insert for batch {}/{}, attempt {}", batchNum, totalBatches, attempt);
                R<io.milvus.grpc.MutationResult> response = milvusClient.insert(insertParam);

                if (response.getStatus() == R.Status.Success.getCode()) {
                    log.info("✅ Batch {}/{} inserted successfully ({} vectors)",
                            batchNum, totalBatches, batch.size());
                    return true;
                } else {
                    String responseMessage = getResponseMessage(response);
                    String errorMsg = String.format("Batch %d/%d insert failed: %s (Status: %d)",
                            batchNum, totalBatches, responseMessage, response.getStatus());
                    log.warn("Attempt {}/{}: {}", attempt, MAX_RETRIES, errorMsg);
                    lastException = new RuntimeException(errorMsg);
                }
            } catch (Exception e) {
                log.warn("Attempt {}/{} for batch {}/{} failed: {}",
                        attempt, MAX_RETRIES, batchNum, totalBatches, e.getMessage());
                lastException = e;
            }

            // Wait before retry
            if (attempt < MAX_RETRIES) {
                try {
                    long waitTime = RETRY_DELAY_MS * attempt;
                    log.debug("Waiting {}ms before retry...", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Log final failure but don't throw - continue with other batches
        log.error("❌ Batch {}/{} failed after {} attempts: {}",
                batchNum, totalBatches, MAX_RETRIES,
                lastException != null ? lastException.getMessage() : "Unknown error");
        return false;
    }

    public boolean hasCollection(String collectionName) {
        try {
            log.debug("Checking if collection {} exists", collectionName);
            HasCollectionParam param = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<Boolean> response = milvusClient.hasCollection(param);

            String responseMessage = getResponseMessage(response);
            log.debug("hasCollection response - Status: {}, Data: {}, Message: {}",
                    response.getStatus(), response.getData(), responseMessage);

            if (response.getStatus() != R.Status.Success.getCode()) {
                log.warn("hasCollection check failed with status {}: {}",
                        response.getStatus(), responseMessage);
                return false;
            }

            return response.getData() != null && response.getData();
        } catch (Exception e) {
            log.error("Error checking collection existence for {}: {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public void dropCollection(String collectionName) {
        try {
            DropCollectionParam param = DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<RpcStatus> response = milvusClient.dropCollection(param);

            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection {} dropped successfully", collectionName);
            }
        } catch (Exception e) {
            log.error("Error dropping collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to drop collection", e);
        }
    }

    /**
     * Converts a metadata map to a JsonObject for Milvus JSON field type.
     * This is the proper way to insert data into a DataType.JSON field in Milvus.
     */
    private JsonObject mapToJsonObject(Map<String, String> map) {
        JsonObject jsonObject = new JsonObject();
        if (map == null || map.isEmpty()) {
            // Return empty JSON object if map is null or empty
            return jsonObject;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                jsonObject.addProperty(key, value != null ? value : "");
            }
        }
        return jsonObject;
    }

    private String mapToJson(Map<String, String> map) {
        // Simple JSON string conversion (kept for backward compatibility)
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (count > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(escapeJson(entry.getValue())).append("\"");
            count++;
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Safely get the message from a Milvus R response.
     * The SDK's getMessage() method can throw NullPointerException when the exception field is null
     * but the status is not Success. This method handles that case safely.
     */
    private <T> String getResponseMessage(R<T> response) {
        if (response == null) {
            return "null response";
        }
        try {
            // Try to get the message directly
            return response.getMessage();
        } catch (NullPointerException e) {
            // The SDK throws NPE when exception is null - return status description instead
            if (response.getException() != null) {
                return response.getException().getMessage();
            }
            return "Status code: " + response.getStatus();
        }
    }
}

