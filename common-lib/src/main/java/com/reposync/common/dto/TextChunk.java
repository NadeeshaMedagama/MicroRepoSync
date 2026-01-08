package com.reposync.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk implements Serializable {
    private String chunkId;
    private String content;
    private Integer chunkIndex;
    private Integer totalChunks;
    private Map<String, String> metadata;
}

