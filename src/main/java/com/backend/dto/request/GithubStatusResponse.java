package com.backend.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GithubStatusResponse {
    private String repositoryId;
    private String message;
    private String status;
    private String cloneStatus;
    private String runStatus;
    private String primaryLanguage;
    private String resultUrl;
    private String githubLink;
    private LocalDateTime timestamp;

    public GithubStatusResponse() {}

    public GithubStatusResponse(String repositoryId, String message, String status, String cloneStatus, String runStatus, String primaryLanguage, String resultUrl, String githubLink, LocalDateTime timestamp) {
        this.repositoryId = repositoryId;
        this.message = message;
        this.status = status;
        this.cloneStatus = cloneStatus;
        this.runStatus = runStatus;
        this.primaryLanguage = primaryLanguage;
        this.resultUrl = resultUrl;
        this.githubLink = githubLink;
        this.timestamp = timestamp;
    }
}