package com.reposync.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobResult implements Serializable {
    private String jobId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer repositoriesProcessed;
    private Integer documentsProcessed;
    private Integer chunksCreated;
    private Integer vectorsStored;
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED
    private String errorMessage;
}

