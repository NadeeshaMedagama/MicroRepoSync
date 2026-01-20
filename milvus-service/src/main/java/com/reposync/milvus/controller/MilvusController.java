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
        log.info("Upserting {} vectors to collection: {}", vectors.size(), collectionName);
        try {
            milvusService.upsertVectors(collectionName, vectors);
            return ResponseEntity.ok("Vectors upserted successfully");
        } catch (Exception e) {
            log.error("Failed to upsert vectors to {}: {}", collectionName, e.getMessage(), e);
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

