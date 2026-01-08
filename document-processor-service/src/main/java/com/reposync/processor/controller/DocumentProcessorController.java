package com.reposync.processor.controller;

import com.reposync.common.dto.DocumentContent;
import com.reposync.common.dto.TextChunk;
import com.reposync.processor.service.ChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/processor")
@RequiredArgsConstructor
public class DocumentProcessorController {

    private final ChunkingService chunkingService;

    @PostMapping("/chunk")
    public ResponseEntity<List<TextChunk>> chunkDocument(@RequestBody DocumentContent document) {
        log.info("Processing document: {} from {}", document.getFileName(), document.getRepositoryName());
        List<TextChunk> chunks = chunkingService.chunkDocument(document);
        log.info("Created {} chunks for document: {}", chunks.size(), document.getFileName());
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/chunk/batch")
    public ResponseEntity<List<TextChunk>> chunkDocuments(@RequestBody List<DocumentContent> documents) {
        log.info("Processing {} documents for chunking", documents.size());
        List<TextChunk> allChunks = chunkingService.chunkDocuments(documents);
        log.info("Created {} total chunks from {} documents", allChunks.size(), documents.size());
        return ResponseEntity.ok(allChunks);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document Processor Service is running");
    }
}

