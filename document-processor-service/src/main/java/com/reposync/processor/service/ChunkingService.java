package com.reposync.processor.service;

import com.reposync.common.dto.DocumentContent;
import com.reposync.common.dto.TextChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChunkingService {

    @Value("${chunking.chunk-size:1000}")
    private int chunkSize;

    @Value("${chunking.overlap:200}")
    private int overlap;

    public List<TextChunk> chunkDocument(DocumentContent document) {
        if (document == null || document.getContent() == null || document.getContent().isEmpty()) {
            log.warn("Empty document provided for chunking");
            return Collections.emptyList();
        }

        List<TextChunk> chunks = new ArrayList<>();
        String content = document.getContent();

        // Split by paragraphs first for better semantic preservation
        List<String> paragraphs = splitIntoParagraphs(content);

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            // If adding this paragraph exceeds chunk size and we have content
            if (currentChunk.length() > 0 &&
                currentChunk.length() + paragraph.length() > chunkSize) {

                // Save current chunk
                chunks.add(createTextChunk(
                    document,
                    currentChunk.toString(),
                    chunkIndex++
                ));

                // Start new chunk with overlap from previous
                String overlapText = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlapText);
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // Add the last chunk if it has content
        if (currentChunk.length() > 0) {
            chunks.add(createTextChunk(
                document,
                currentChunk.toString().trim(),
                chunkIndex
            ));
        }

        // Update total chunks count in all chunks
        int totalChunks = chunks.size();
        chunks.forEach(chunk -> chunk.setTotalChunks(totalChunks));

        return chunks;
    }

    public List<TextChunk> chunkDocuments(List<DocumentContent> documents) {
        return documents.stream()
                .flatMap(doc -> chunkDocument(doc).stream())
                .collect(Collectors.toList());
    }

    private List<String> splitIntoParagraphs(String content) {
        // Split by double newlines, single newlines, or common section markers
        String[] parts = content.split("\\n\\s*\\n|(?=#{1,6}\\s)|(?=##)");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String getOverlapText(String text) {
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }

    private TextChunk createTextChunk(DocumentContent document, String content, int index) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("repository", document.getRepositoryName());
        metadata.put("file_path", document.getFilePath());
        metadata.put("file_name", document.getFileName());
        metadata.put("file_type", document.getFileType());
        metadata.put("sha", document.getSha());

        String chunkId = String.format("%s_%s_%d",
            document.getRepositoryName().replace("/", "_"),
            document.getFileName(),
            index
        );

        return TextChunk.builder()
                .chunkId(chunkId)
                .content(content)
                .chunkIndex(index)
                .metadata(metadata)
                .build();
    }
}

