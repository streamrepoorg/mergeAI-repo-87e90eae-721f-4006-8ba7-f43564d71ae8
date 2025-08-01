package com.backend.controller.github;

import com.backend.config.exception.GithubNotFoundException;
import com.backend.config.exception.GithubProcessingException;
import com.backend.dto.request.GithubProcessRequest;
import com.backend.dto.response.ResponseDetails;
import com.backend.service.github.GithubServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/github")
@Slf4j
public class GithubController {

    @Autowired
    private GithubServiceImpl githubService;

    @PostMapping("/process")
    public ResponseEntity<?> processRepository(@Valid @RequestBody GithubProcessRequest request) {
        log.info("Processing GitHub repository: {}", request.getGithubLink());
        String repoId = githubService.processRepository(request.getGithubLink());
        ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Repository processing initiated successfully with ID: " + repoId, HttpStatus.ACCEPTED.toString());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDetails);
    }

    @GetMapping("/status/{repositoryId}")
    public ResponseEntity<?> getRepositoryStatus(@PathVariable String repositoryId) {
        String status = githubService.getRepositoryStatus(repositoryId);
        ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), status, HttpStatus.OK.toString());
        return ResponseEntity.ok(responseDetails);
    }

}