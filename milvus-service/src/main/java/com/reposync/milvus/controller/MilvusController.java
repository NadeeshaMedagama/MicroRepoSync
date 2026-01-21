package com.reposync.milvus.controller;

import com.reposync.common.dto.EmbeddingVector;
import com.reposync.milvus.service.MilvusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/milvus")
@RequiredArgsConstructor
public class MilvusController {

    private final MilvusService milvusService;

    @PostMapping("/collection/create")
    public ResponseEntity<String> createCollection(@RequestParam String collectionName,
                                                    @RequestParam int dimension) {
        log.info("Creating collection: {} with dimension: {}", collectionName, dimension);
        try {
            milvusService.createCollection(collectionName, dimension);
            return ResponseEntity.ok("Collection created successfully");
        } catch (Exception e) {
            log.error("Failed to create collection {}: {}", collectionName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to create collection: " + e.getMessage());
        }
    }

    @PostMapping("/vectors/upsert")
    public ResponseEntity<String> upsertVectors(@RequestParam String collectionName,
                                                 @RequestBody List<EmbeddingVector> vectors) {
        log.info("Received upsert request: {} vectors to collection: {}", vectors.size(), collectionName);
        long startTime = System.currentTimeMillis();
        try {
            // Log sample vector info
            if (!vectors.isEmpty()) {
                EmbeddingVector first = vectors.get(0);
                log.debug("First vector: id={}, dimension={}",
                        first.getId(),
                        first.getVector() != null ? first.getVector().size() : 0);
            }

            milvusService.upsertVectors(collectionName, vectors);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully upserted {} vectors to {} in {}ms", vectors.size(), collectionName, duration);
            return ResponseEntity.ok("Vectors upserted successfully (" + vectors.size() + " vectors in " + duration + "ms)");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to upsert vectors to {} after {}ms: {}", collectionName, duration, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to upsert vectors: " + e.getMessage());
        }
    }

    @DeleteMapping("/collection/{collectionName}")
    public ResponseEntity<String> dropCollection(@PathVariable String collectionName) {
        log.info("Dropping collection: {}", collectionName);
        milvusService.dropCollection(collectionName);
        return ResponseEntity.ok("Collection dropped successfully");
    }

    @GetMapping("/collection/{collectionName}/exists")
    public ResponseEntity<Boolean> collectionExists(@PathVariable String collectionName) {
        boolean exists = milvusService.hasCollection(collectionName);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Milvus Service is running");
    }
}

