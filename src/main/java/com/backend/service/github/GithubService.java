package com.backend.service.github;

import com.backend.dto.request.GithubStatusResponse;

public interface GithubService {
    String processRepository(String githubLink);
    GithubStatusResponse getRepositoryStatus(String repositoryId);
}
