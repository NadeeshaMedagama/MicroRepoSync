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
public class RepositoryInfo implements Serializable {
    private String name;
    private String fullName;
    private String description;
    private String url;
    private String defaultBranch;
    private String language;
    private Long stargazersCount;
    private Long forksCount;
    private String updatedAt;
}

