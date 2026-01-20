package com.reposync.milvus.service;

import com.reposync.common.dto.EmbeddingVector;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.response.DescCollResponseWrapper;
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
            // Check if collection already exists
            if (hasCollection(collectionName)) {
                log.info("Collection {} already exists", collectionName);
                return;
            }

            log.info("Creating collection {} with dimension {}", collectionName, dimension);

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

            // Use VarChar for metadata instead of JSON for better compatibility with Zilliz Cloud
            FieldType metadataField = FieldType.newBuilder()
                    .withName(METADATA_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
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

            log.info("Milvus createCollection response - Status: {}, Message: {}",
                    response.getStatus(), response.getMessage());

            if (response.getStatus() != R.Status.Success.getCode()) {
                String errorMsg = String.format("Milvus createCollection failed with status %d: %s",
                        response.getStatus(), response.getMessage());
                log.error(errorMsg);
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
            if (vectors.isEmpty()) {
                log.warn("No vectors to upsert");
                return;
            }

            // Ensure collection exists
            if (!hasCollection(collectionName)) {
                int dimension = vectors.get(0).getVector().size();
                createCollection(collectionName, dimension);
            }

            // Prepare data for insertion
            List<String> ids = vectors.stream()
                    .map(EmbeddingVector::getId)
                    .collect(Collectors.toList());

            List<List<Float>> vectorList = vectors.stream()
                    .map(EmbeddingVector::getVector)
                    .collect(Collectors.toList());

            List<Map<String, String>> metadataList = vectors.stream()
                    .map(EmbeddingVector::getMetadata)
                    .collect(Collectors.toList());

            // Convert metadata to JSON strings
            List<String> jsonMetadata = metadataList.stream()
                    .map(this::mapToJson)
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
                throw new RuntimeException("Failed to insert vectors: " + response.getMessage());
            }

            log.info("Successfully upserted {} vectors to collection {}", vectors.size(), collectionName);

        } catch (Exception e) {
            log.error("Error upserting vectors to {}: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vectors", e);
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

    private String mapToJson(Map<String, String> map) {
        // Simple JSON conversion
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

