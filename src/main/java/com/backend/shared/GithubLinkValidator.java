package com.backend.shared;

import com.backend.config.exception.GithubNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Service
@Slf4j
public class GithubLinkValidator {

    @Autowired
    private RestTemplate restTemplate;

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String REPO_PATTERN = "^https?://(www\\.)?github\\.com/([a-zA-Z0-9-]+)/([a-zA-Z0-9-_]+)$";
    private static final String USER_PATTERN = "^https?://(www\\.)?github\\.com/([a-zA-Z0-9-]+)$";

    public boolean isValidGitHubLink(String link) {
        if (link == null || link.trim().isEmpty()) {
            throw new GithubNotFoundException("GitHub link cannot be null or empty");
        }
        String normalizedLink = link.trim().replaceAll("/+$", "");
        if (Pattern.matches(REPO_PATTERN, normalizedLink)) {
            return !validateRepositoryLink(normalizedLink);
        }
        else if (Pattern.matches(USER_PATTERN, normalizedLink)) {
            return !validateUserLink(normalizedLink);
        }
        else {
            throw new GithubNotFoundException("Invalid GitHub link format: " + link);
        }
    }

    private boolean validateRepositoryLink(String link) {
        try {
            String[] parts = link.split("/");
            String owner = parts[parts.length - 2];
            String repo = parts[parts.length - 1];
            String apiUrl = String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, owner, repo);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            log.info("Validated repository link: {} - Status: {}", link, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            log.error("Failed to validate repository link: {} - Status: {}", link, e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating repository link: {} - Error: {}", link, e.getMessage());
            return false;
        }
    }

    private boolean validateUserLink(String link) {
        try {
            String[] parts = link.split("/");
            String username = parts[parts.length - 1];
            String apiUrl = String.format("%s/users/%s", GITHUB_API_BASE_URL, username);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            log.info("Validated user link: {} - Status: {}", link, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            log.error("Failed to validate user link: {} - Status: {}", link, e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating user link: {} - Error: {}", link, e.getMessage());
            return false;
        }
    }
}