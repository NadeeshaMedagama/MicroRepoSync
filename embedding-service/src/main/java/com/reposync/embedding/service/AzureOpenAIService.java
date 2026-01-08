package com.reposync.embedding.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.reposync.common.dto.EmbeddingVector;
import com.reposync.common.dto.TextChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureOpenAIService {

    private final OpenAIClient openAIClient;

    @Value("${azure.openai.embeddings-deployment}")
    private String embeddingsDeployment;

    @Value("${azure.openai.batch-size:16}")
    private int batchSize;

    public EmbeddingVector generateEmbedding(TextChunk chunk) {
        try {
            EmbeddingsOptions options = new EmbeddingsOptions(List.of(chunk.getContent()));
            Embeddings embeddings = openAIClient.getEmbeddings(embeddingsDeployment, options);

            if (embeddings.getData().isEmpty()) {
                throw new RuntimeException("No embeddings generated for chunk: " + chunk.getChunkId());
            }

            EmbeddingItem item = embeddings.getData().get(0);
            List<Float> vector = convertToFloatList(item.getEmbedding());

            return EmbeddingVector.builder()
                    .id(chunk.getChunkId())
                    .vector(vector)
                    .metadata(chunk.getMetadata())
                    .build();

        } catch (Exception e) {
            log.error("Error generating embedding for chunk {}: {}", chunk.getChunkId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public List<EmbeddingVector> generateEmbeddings(List<TextChunk> chunks) {
        List<EmbeddingVector> results = new ArrayList<>();

        // Process in batches to avoid rate limits and memory issues
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunks.size());
            List<TextChunk> batch = chunks.subList(i, endIndex);

            log.info("Processing batch {}/{}: chunks {}-{}",
                    (i / batchSize) + 1,
                    (chunks.size() + batchSize - 1) / batchSize,
                    i, endIndex - 1);

            List<EmbeddingVector> batchResults = processBatch(batch);
            results.addAll(batchResults);

            // Small delay between batches to avoid rate limiting
            if (endIndex < chunks.size()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return results;
    }

    private List<EmbeddingVector> processBatch(List<TextChunk> batch) {
        try {
            List<String> texts = batch.stream()
                    .map(TextChunk::getContent)
                    .collect(Collectors.toList());

            EmbeddingsOptions options = new EmbeddingsOptions(texts);
            Embeddings embeddings = openAIClient.getEmbeddings(embeddingsDeployment, options);

            List<EmbeddingVector> vectors = new ArrayList<>();
            List<EmbeddingItem> embeddingItems = embeddings.getData();

            for (int i = 0; i < batch.size(); i++) {
                TextChunk chunk = batch.get(i);
                EmbeddingItem item = embeddingItems.get(i);

                List<Float> vector = convertToFloatList(item.getEmbedding());

                vectors.add(EmbeddingVector.builder()
                        .id(chunk.getChunkId())
                        .vector(vector)
                        .metadata(chunk.getMetadata())
                        .build());
            }

            return vectors;

        } catch (Exception e) {
            log.error("Error processing batch: {}", e.getMessage(), e);
            // Fallback to individual processing if batch fails
            return batch.stream()
                    .map(this::generateEmbedding)
                    .collect(Collectors.toList());
        }
    }

    private List<Float> convertToFloatList(List<Double> doubleList) {
        return doubleList.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
    }
}

