package com.reposync.embedding.controller;

import com.reposync.common.dto.EmbeddingVector;
import com.reposync.common.dto.TextChunk;
import com.reposync.embedding.service.AzureOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final AzureOpenAIService azureOpenAIService;

    @PostMapping("/generate")
    public ResponseEntity<EmbeddingVector> generateEmbedding(@RequestBody TextChunk chunk) {
        log.info("Generating embedding for chunk: {}", chunk.getChunkId());
        EmbeddingVector vector = azureOpenAIService.generateEmbedding(chunk);
        return ResponseEntity.ok(vector);
    }

    @PostMapping("/generate/batch")
    public ResponseEntity<List<EmbeddingVector>> generateEmbeddings(@RequestBody List<TextChunk> chunks) {
        log.info("Generating embeddings for {} chunks", chunks.size());
        List<EmbeddingVector> vectors = azureOpenAIService.generateEmbeddings(chunks);
        log.info("Generated {} embeddings", vectors.size());
        return ResponseEntity.ok(vectors);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Embedding Service is running");
    }
}

