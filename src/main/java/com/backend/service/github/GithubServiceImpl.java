package com.backend.service.github;

import com.backend.config.CloudinaryConfig;
import com.backend.dto.request.GithubStatusResponse;
import com.backend.shared.exception.GithubNotFoundException;
import com.backend.shared.exception.GithubProcessingException;
import com.backend.model.github.GitStatus;
import com.backend.model.github.Github;
import com.backend.repository.github.GithubRepository;
import com.backend.config.GithubLinkValidator;
import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class GithubServiceImpl implements GithubService {

    private final GithubRepository githubRepository;
    private final RestTemplate restTemplate;
    private final GithubLinkValidator gitLinkValidator;
    private final ObjectMapper objectMapper;
    private final Cloudinary cloudinary;
    private final SimpMessagingTemplate messagingTemplate;
    private final String cloneBasePath;

    public GithubServiceImpl(
            GithubRepository githubRepository,
            RestTemplate restTemplate,
            GithubLinkValidator gitLinkValidator,
            SimpMessagingTemplate messagingTemplate,
            CloudinaryConfig cloudinaryConfig,
            @Value("${github.clone.base-path:/tmp/github-clones}") String cloneBasePath
    ) {
        this.githubRepository = githubRepository;
        this.restTemplate = restTemplate;
        this.gitLinkValidator = gitLinkValidator;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
        this.cloneBasePath = cloneBasePath;

        if (cloudinaryConfig.getCloudName() == null || cloudinaryConfig.getApiKey() == null || cloudinaryConfig.getApiSecret() == null) {
            throw new IllegalStateException("Missing Cloudinary configuration properties");
        }

        this.cloudinary = new Cloudinary(Map.of(
                "cloud_name", cloudinaryConfig.getCloudName(),
                "api_key", cloudinaryConfig.getApiKey(),
                "api_secret", cloudinaryConfig.getApiSecret()
        ));
        createCloneBasePath();
    }

    private void createCloneBasePath() {
        Path basePath = Paths.get(cloneBasePath);
        try {
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("Created clone base directory: {}", cloneBasePath);
            }
        } catch (IOException e) {
            log.error("Failed to create clone base directory {}: {}", cloneBasePath, e.getMessage(), e);
            throw new IllegalStateException("Cannot create clone base directory: " + e.getMessage());
        }
    }

    @Override
    public String processRepository(String githubLink) {
        try {
            if (!gitLinkValidator.isValidGitHubLink(githubLink)) {
                throw new IllegalArgumentException("Invalid GitHub link: " + githubLink);
            }
            String repositoryId = UUID.randomUUID().toString();
            Github github = new Github();
            github.setId(repositoryId);
            github.setGithubLink(githubLink);
            github.setCloneGitStatus(GitStatus.PENDING);
            github.setRunGitStatus(GitStatus.PENDING);
            github.setCreatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
            github.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
            githubRepository.save(github);
            processRepositoryAsync(repositoryId);
            log.info("Initiated processing for repository ID: {}", repositoryId);
            return repositoryId;
        } catch (Exception e) {
            log.error("Failed to initiate repository processing: {}", e.getMessage(), e);
            throw new GithubProcessingException("Failed to initiate repository processing: " + e.getMessage());
        }
    }

    @Override
    public GithubStatusResponse getRepositoryStatus(String repositoryId) {
        try {
            Github repository = githubRepository.findById(repositoryId)
                    .orElseThrow(() -> new GithubNotFoundException("Repository not found with ID: " + repositoryId));
            return new GithubStatusResponse(
                    repositoryId,
                    "Repository status retrieved successfully",
                    "SUCCESS",
                    repository.getCloneGitStatus().toString(),
                    repository.getRunGitStatus().toString(),
                    repository.getPrimaryLanguage() != null ? repository.getPrimaryLanguage() : "Unknown",
                    repository.getResultUrl(),
                    repository.getGithubLink(),
                    LocalDateTime.now()
            );
        } catch (GithubNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch status for repository {}: {}", repositoryId, e.getMessage(), e);
            throw new GithubProcessingException("Failed to fetch repository status: " + e.getMessage());
        }
    }

    @Async
    public void processRepositoryAsync(String repositoryId) {
        Github repository = githubRepository.findById(repositoryId)
                .orElseThrow(() -> new GithubNotFoundException("Repository not found with ID: " + repositoryId));
        String clonePath = null;
        try {
            sendWebSocketUpdate(repositoryId, "Starting repository processing...", "INFO");
            if (!gitLinkValidator.isValidGitHubLink(repository.getGithubLink())) {
                updateRepositoryStatus(repository, GitStatus.FAILED, GitStatus.FAILED);
                throw new GithubProcessingException("Invalid GitHub link: " + repository.getGithubLink());
            }
            clonePath = cloneBasePath + File.separator + repositoryId;
            File cloneDir = new File(clonePath);
            if (!cloneDir.mkdirs()) {
                throw new IOException("Failed to create clone directory: " + clonePath);
            }

            sendWebSocketUpdate(repositoryId, "Cloning repository...", "INFO");
            cloneRepository(repository, clonePath, cloneDir);
            repository.setCloneGitStatus(GitStatus.SUCCESS);

            sendWebSocketUpdate(repositoryId, "Detecting project languages...", "INFO");
            String languages = detectLanguages(repository.getGithubLink());
            repository.setPrimaryLanguage(getPrimaryLanguage(languages));

            sendWebSocketUpdate(repositoryId, "Processing project...", "INFO");
            String resultFilePath = processProject(cloneDir, repository.getPrimaryLanguage(), repositoryId);

            sendWebSocketUpdate(repositoryId, "Uploading results to Cloudinary...", "INFO");
            String cloudinaryUrl = uploadToCloudinary(resultFilePath, repositoryId);
            repository.setResultUrl(cloudinaryUrl);
            repository.setRunGitStatus(GitStatus.SUCCESS);
            repository.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));

            githubRepository.save(repository);
            sendWebSocketUpdate(repositoryId, "Processing complete! Results available at: " + cloudinaryUrl, "SUCCESS");
            log.info("Successfully processed repository: {}", repository.getGithubLink());
        } catch (Exception e) {
            updateRepositoryStatus(repository, GitStatus.FAILED, GitStatus.FAILED);
            sendWebSocketUpdate(repositoryId, "Error: " + e.getMessage(), "ERROR");
            log.error("Failed to process repository {}: {}", repository.getGithubLink(), e.getMessage(), e);
            throw new GithubProcessingException("Failed to process repository: " + e.getMessage());
        } finally {
            if (clonePath != null) {
                cleanupCloneDirectory(clonePath);
            }
        }
    }

    private void cloneRepository(Github repository, String clonePath, File cloneDir) {
        try {
            Git.cloneRepository()
                    .setURI(repository.getGithubLink())
                    .setDirectory(cloneDir)
                    .call();
            log.info("Successfully cloned repository {} to {}", repository.getGithubLink(), clonePath);
        } catch (Exception e) {
            log.error("Failed to clone repository {}: {}", repository.getGithubLink(), e.getMessage(), e);
            throw new GithubProcessingException("Failed to clone repository: " + e.getMessage());
        }
    }

    private String detectLanguages(String githubLink) {
        try {
            String[] parts = githubLink.split("/");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid GitHub URL format");
            }
            String owner = parts[parts.length - 2];
            String repo = parts[parts.length - 1].replace(".git", "");
            String apiUrl = String.format("https://api.github.com/repos/%s/%s/languages", owner, repo);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String languages = objectMapper.writeValueAsString(response.getBody());
                log.info("Detected languages for {}: {}", githubLink, languages);
                return languages;
            }
            log.warn("No languages detected for {}", githubLink);
            return "{}";
        } catch (Exception e) {
            log.error("Failed to detect languages for {}: {}", githubLink, e.getMessage(), e);
            throw new GithubProcessingException("Failed to detect languages: " + e.getMessage());
        }
    }

    private String getPrimaryLanguage(String languageJson) {
        try {
            Map<String, Long> languages = objectMapper.readValue(languageJson, Map.class);
            String primaryLanguage = languages.keySet().stream()
                    .max((lang1, lang2) -> Long.compare(languages.get(lang1), languages.get(lang2)))
                    .orElse("Unknown");
            log.info("Primary language detected: {}", primaryLanguage);
            return primaryLanguage;
        } catch (Exception e) {
            log.error("Failed to parse primary language: {}", e.getMessage(), e);
            return "Unknown";
        }
    }

    private String processProject(File repoDir, String primaryLanguage, String repositoryId) throws IOException {
        String resultFilePath = cloneBasePath + File.separator + repositoryId + File.separator + "results.txt";
        File resultFile = new File(resultFilePath);
        if (!resultFile.getParentFile().mkdirs() && !resultFile.getParentFile().exists()) {
            throw new IOException("Failed to create results directory for: " + resultFilePath);
        }

        try (FileWriter writer = new FileWriter(resultFile)) {
            String projectType = detectProjectType(repoDir);
            writer.write("Repository ID: " + repositoryId + "\n");
            writer.write("Primary Language: " + primaryLanguage + "\n");
            writer.write("Project Type: " + projectType + "\n");
            writer.write("Processing Log:\n");

            switch (projectType.toLowerCase()) {
                case "web":
                    writer.write("Detected web project. Generating preview...\n");
                    writer.write("Preview generated (placeholder).\n");
                    break;
                case "api":
                    writer.write("Detected API project. Analyzing endpoints...\n");
                    Map<String, String> endpoints = detectEndpoints(repoDir);
                    for (Map.Entry<String, String> entry : endpoints.entrySet()) {
                        writer.write("Endpoint: " + entry.getKey() + ", Status: " + entry.getValue() + "\n");
                    }
                    break;
                case "mobile":
                    writer.write("Detected mobile project. Generating preview...\n");
                    writer.write("Mobile preview generated (placeholder).\n");
                    break;
                default:
                    writer.write("Unknown project type. No specific actions taken.\n");
            }
            log.info("Generated results file for repository {} at {}", repositoryId, resultFilePath);
            return resultFilePath;
        } catch (IOException e) {
            log.error("Failed to process project for repository {}: {}", repositoryId, e.getMessage(), e);
            throw e;
        }
    }

    private String detectProjectType(File repoDir) {
        Path repoPath = repoDir.toPath();
        if (Files.exists(repoPath.resolve("package.json"))) {
            return "web";
        } else if (Files.exists(repoPath.resolve("pom.xml")) || Files.exists(repoPath.resolve("openapi.yaml"))) {
            return "api";
        } else if (Files.exists(repoPath.resolve("AndroidManifest.xml")) || Files.exists(repoPath.resolve("Info.plist"))) {
            return "mobile";
        }
        return "unknown";
    }

    private Map<String, String> detectEndpoints(File repoDir) {
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("/api/sample", "Not Tested");
        return endpoints;
    }

    private String uploadToCloudinary(String filePath, String repositoryId) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IOException("Result file does not exist: " + filePath);
            }
            Map uploadResult = cloudinary.uploader().upload(file, Map.of(
                    "public_id", "repo_results/" + repositoryId,
                    "resource_type", "raw"
            ));
            String url = (String) uploadResult.get("secure_url");
            log.info("Uploaded results for repository {} to Cloudinary: {}", repositoryId, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload to Cloudinary for repository {}: {}", repositoryId, e.getMessage(), e);
            throw new GithubProcessingException("Failed to upload results to Cloudinary: " + e.getMessage());
        }
    }

    private void sendWebSocketUpdate(String repositoryId, String message, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("repositoryId", repositoryId);
            payload.put("message", message);
            payload.put("status", status);
            payload.put("timestamp", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
            String destination = "/topic/repo/" + repositoryId;
            messagingTemplate.convertAndSend(destination, objectMapper.writeValueAsString(payload));
            log.info("WebSocket [{}] [{}]: {}", repositoryId, status, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update for repository {}: {}", repositoryId, e.getMessage(), e);
        }
    }

    private void updateRepositoryStatus(Github repository, GitStatus cloneStatus, GitStatus runStatus) {
        repository.setCloneGitStatus(cloneStatus);
        repository.setRunGitStatus(runStatus);
        repository.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        githubRepository.save(repository);
        log.info("Updated repository status for {}: clone={}, run={}", repository.getId(), cloneStatus, runStatus);
    }

    private void cleanupCloneDirectory(String clonePath) {
        try {
            Path path = Paths.get(clonePath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted((p1, p2) -> -p1.compareTo(p2))
                        .map(Path::toFile)
                        .forEach(file -> {
                            if (!file.delete()) {
                                log.warn("Failed to delete file: {}", file.getAbsolutePath());
                            }
                        });
                log.info("Cleaned up clone directory: {}", clonePath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup clone directory {}: {}", clonePath, e.getMessage());
        }
    }
}