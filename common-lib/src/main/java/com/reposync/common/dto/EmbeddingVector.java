package com.reposync.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingVector implements Serializable {
    private String id;
    private List<Float> vector;
    private Map<String, String> metadata;
}

