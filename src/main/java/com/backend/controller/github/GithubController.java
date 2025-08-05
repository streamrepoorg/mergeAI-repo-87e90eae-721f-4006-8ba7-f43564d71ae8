package com.backend.controller.github;

import com.backend.dto.request.GithubStatusResponse;
import com.backend.dto.response.ResponseDetails;
import com.backend.service.github.GithubService;
import com.backend.shared.exception.GithubNotFoundException;
import com.backend.shared.exception.GithubProcessingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/github")
@Slf4j
public class GithubController {

    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @PostMapping("/process")
    public ResponseEntity<ResponseDetails> processRepository(@Valid @RequestBody GithubStatusResponse request) {
        log.info("Processing GitHub repository: {}", request.getGithubLink());
        try {
            String repoId = githubService.processRepository(request.getGithubLink());
            ResponseDetails response = new ResponseDetails(LocalDateTime.now(), "Repository processing initiated successfully with ID: " + repoId, HttpStatus.ACCEPTED.toString(), "/api/github/status/" + repoId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid GitHub URL: {}", request.getGithubLink(), e);
            ResponseDetails error = new ResponseDetails(LocalDateTime.now(), "Invalid GitHub URL: " + e.getMessage(), HttpStatus.BAD_REQUEST.toString(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (GithubProcessingException e) {
            log.error("Failed to initiate repository processing: {}", e.getMessage(), e);
            ResponseDetails error = new ResponseDetails(LocalDateTime.now(), "Failed to initiate repository processing: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/status/{repositoryId}")
    public ResponseEntity<GithubStatusResponse> getRepositoryStatus(@PathVariable String repositoryId) {
        log.info("Fetching status for repository ID: {}", repositoryId);
        try {
            GithubStatusResponse status = githubService.getRepositoryStatus(repositoryId);
            return ResponseEntity.ok(status);
        } catch (GithubNotFoundException e) {
            log.error("Repository not found: {}", repositoryId, e);
            GithubStatusResponse error = new GithubStatusResponse(repositoryId, "Repository not found: " + e.getMessage(), "ERROR", null, null, null, null, null, LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (GithubProcessingException e) {
            log.error("Failed to fetch repository status: {}", e.getMessage(), e);
            GithubStatusResponse error = new GithubStatusResponse(repositoryId, "Failed to fetch repository status: " + e.getMessage(), "ERROR", null, null, null, null, null, LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}