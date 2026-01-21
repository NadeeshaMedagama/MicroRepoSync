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
                    log.error("Milvus connection failed: {}", listResponse.getMessage());
                    throw new RuntimeException("Cannot connect to Milvus: " + listResponse.getMessage());
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

            log.info("Milvus createCollection response - Status: {}, StatusCode: {}, Message: {}, Exception: {}",
                    response.getStatus(),
                    response.getStatus(),
                    response.getMessage(),
                    response.getException() != null ? response.getException().getMessage() : "none");

            if (response.getStatus() != R.Status.Success.getCode()) {
                String errorMsg = String.format("Milvus createCollection failed with status %d: %s (Exception: %s)",
                        response.getStatus(),
                        response.getMessage(),
                        response.getException() != null ? response.getException().getMessage() : "none");
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
        try {
            log.info("Creating index for collection {} on field {}", collectionName, VECTOR_FIELD);

            // Use AUTOINDEX for Zilliz Cloud compatibility
            // For self-hosted Milvus, this will create an appropriate index automatically
            io.milvus.param.index.CreateIndexParam indexParam = io.milvus.param.index.CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(VECTOR_FIELD)
                    .withIndexName("vector_idx")
                    .withIndexType(io.milvus.param.IndexType.AUTOINDEX)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .build();

            R<RpcStatus> response = milvusClient.createIndex(indexParam);

            log.info("Milvus createIndex response - Status: {}, Message: {}",
                    response.getStatus(), response.getMessage());

            if (response.getStatus() != R.Status.Success.getCode()) {
                // On Zilliz Cloud Serverless, index might be auto-created, so just log warning
                log.warn("Index creation returned non-success status (may be auto-indexed): {}", response.getMessage());
            } else {
                log.info("Index created successfully for collection {}", collectionName);
            }
        } catch (Exception e) {
            // Don't fail if index creation fails - Zilliz Cloud Serverless auto-indexes
            log.warn("Index creation exception (may be auto-indexed on Zilliz Cloud): {}", e.getMessage());
        }
    }

    private void loadCollection(String collectionName) {
        try {
            log.info("Loading collection {} into memory", collectionName);

            LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<RpcStatus> response = milvusClient.loadCollection(loadParam);

            log.info("Milvus loadCollection response - Status: {}, Message: {}",
                    response.getStatus(), response.getMessage());

            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection {} loaded into memory", collectionName);
            } else {
                // On Zilliz Cloud Serverless, collections are auto-loaded
                log.warn("Load collection returned non-success (may be auto-loaded): {}", response.getMessage());
            }
        } catch (Exception e) {
            // Don't fail if load fails - Zilliz Cloud Serverless auto-loads
            log.warn("Load collection exception (may be auto-loaded on Zilliz Cloud): {}", e.getMessage());
        }
    }

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

            // Ensure collection exists
            if (!hasCollection(collectionName)) {
                int dimension = validVectors.get(0).getVector().size();
                createCollection(collectionName, dimension);
            }

            // Prepare data for insertion
            List<String> ids = validVectors.stream()
                    .map(EmbeddingVector::getId)
                    .collect(Collectors.toList());

            List<List<Float>> vectorList = validVectors.stream()
                    .map(EmbeddingVector::getVector)
                    .collect(Collectors.toList());

            // Convert metadata to JsonObject for Milvus JSON field type
            List<JsonObject> jsonMetadata = validVectors.stream()
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

            R<io.milvus.grpc.MutationResult> response = milvusClient.insert(insertParam);

            if (response.getStatus() != R.Status.Success.getCode()) {
                String errorMsg = String.format("Failed to insert vectors: %s (Status: %d, Exception: %s)",
                        response.getMessage(),
                        response.getStatus(),
                        response.getException() != null ? response.getException().getMessage() : "none");
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            log.info("Successfully upserted {} vectors to collection {}", validVectors.size(), collectionName);

        } catch (Exception e) {
            log.error("Error upserting vectors to {}: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vectors: " + e.getMessage(), e);
        }
    }

    public boolean hasCollection(String collectionName) {
        try {
            log.debug("Checking if collection {} exists", collectionName);
            HasCollectionParam param = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<Boolean> response = milvusClient.hasCollection(param);

            log.debug("hasCollection response - Status: {}, Data: {}, Message: {}",
                    response.getStatus(), response.getData(), response.getMessage());

            if (response.getStatus() != R.Status.Success.getCode()) {
                log.warn("hasCollection check failed with status {}: {}",
                        response.getStatus(), response.getMessage());
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
}

