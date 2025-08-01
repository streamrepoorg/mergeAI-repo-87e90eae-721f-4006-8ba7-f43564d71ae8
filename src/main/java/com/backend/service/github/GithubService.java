package com.backend.service.github;

public interface GithubService {
    String getRepositoryStatus(String repositoryId);
    String processRepository(String githubLink);
}
