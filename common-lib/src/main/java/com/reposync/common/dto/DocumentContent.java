package com.reposync.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContent implements Serializable {
    private String repositoryName;
    private String filePath;
    private String fileName;
    private String fileType; // README or API_DEFINITION
    private String content;
    private String sha;
}

