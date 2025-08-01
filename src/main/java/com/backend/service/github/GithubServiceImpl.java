package com.backend.service.github;

import com.backend.config.exception.GithubNotFoundException;
import com.backend.config.exception.GithubProcessingException;
import com.backend.model.github.GitStatus;
import com.backend.model.github.Github;
import com.backend.repository.github.GithubRepository;
import com.backend.shared.GithubLinkValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GithubServiceImpl implements GithubService {

    private final GithubRepository githubRepository;
    private final RestTemplate restTemplate;
    private final GithubLinkValidator gitLinkValidator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DockerClient dockerClient;

    @Value("${clone.directory}")
    private String cloneBasePath;

    @Value("${kafka.topic:repository-processing}")
    private String kafkaTopic;

    @Value("${docker.image.javascript:node:16}")
    private String jsDockerImage;

    @Value("${docker.image.java:openjdk:11}")
    private String javaDockerImage;

    @Value("${docker.image.python:python:3.9}")
    private String pythonDockerImage;

    @Value("${docker.host:tcp://localhost:2375}")
    private String dockerHost;

    public GithubServiceImpl(
            GithubRepository githubRepository,
            RestTemplate restTemplate,
            GithubLinkValidator gitLinkValidator,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.githubRepository = githubRepository;
        this.restTemplate = restTemplate;
        this.gitLinkValidator = gitLinkValidator;
        this.kafkaTemplate = kafkaTemplate;
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();
            this.dockerClient = DockerClientBuilder.getInstance(config).build();
        } catch (Exception e) {
            log.error("Failed to initialize Docker client: {}", e.getMessage(), e);
            throw new RuntimeException("Docker client initialization failed", e);
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
            kafkaTemplate.send(kafkaTopic, repositoryId);
            log.info("Sent repository {} to Kafka for processing", repositoryId);
            return repositoryId;
        } catch (Exception e) {
            log.error("Failed to process repository: {}", e.getMessage(), e);
            throw new GithubProcessingException("Failed to process repository: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "repo-processor")
    private void processRepositoryAsync(String repositoryId) {
        Github repository = githubRepository.findById(repositoryId)
                .orElseThrow(() -> new GithubNotFoundException("Repository not found with id: " + repositoryId));
        String clonePath = null;
        try {
            if (!gitLinkValidator.isValidGitHubLink(repository.getGithubLink())) {
                updateRepositoryStatus(repository);
                throw new GithubProcessingException("Invalid GitHub link: " + repository.getGithubLink());
            }
            clonePath = cloneBasePath + "/" + repositoryId;
            File cloneDir = new File(clonePath);
            if (!cloneDir.mkdirs()) {
                throw new IOException("Failed to create clone directory: " + clonePath);
            }
            cloneRepositoryWithRetry(repository, clonePath, cloneDir);
            repository.setCloneGitStatus(GitStatus.SUCCESS);
            String languages = detectLanguagesWithRetry(repository.getGithubLink());
            repository.setPrimaryLanguage(languages);
            String primaryLanguage = getPrimaryLanguage(languages);
            repository.setPrimaryLanguage(primaryLanguage);
            String runStatus = runProjectInDocker(cloneDir, primaryLanguage);
            repository.setRunGitStatus(GitStatus.valueOf(runStatus));
            repository.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
            githubRepository.save(repository);
            log.info("Successfully processed repository: {}", repository.getGithubLink());
        } catch (Exception e) {
            updateRepositoryStatus(repository);
            log.error("Failed to process repository {}: {}", repository.getGithubLink(), e.getMessage(), e);
            throw new GithubProcessingException("Failed to process repository: " + e.getMessage());
        } finally {
            if (clonePath != null) {
                cleanupCloneDirectory(clonePath);
            }
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private void cloneRepositoryWithRetry(Github repository, String clonePath, File cloneDir) {
        try {
            Git.cloneRepository()
                    .setURI(repository.getGithubLink())
                    .setDirectory(cloneDir)
                    .call();
        } catch (Exception e) {
            log.error("Clone attempt failed for {}: {}", repository.getGithubLink(), e.getMessage(), e);
            throw new GithubProcessingException("Clone failed: " + e.getMessage());
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private String detectLanguagesWithRetry(String githubLink) {
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
                    apiUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.writeValueAsString(response.getBody());
            }
            return "{}";
        } catch (Exception e) {
            log.error("Failed to detect languages for {}: {}", githubLink, e.getMessage(), e);
            throw new GithubProcessingException("Language detection failed: " + e.getMessage());
        }
    }

    @Override
    public String getRepositoryStatus(String repositoryId) {
        try {
            Github repository = githubRepository.findById(repositoryId)
                    .orElseThrow(() -> new GithubNotFoundException("Repository not found with ID: " + repositoryId));
            return String.format(
                    "Clone Status: %s | Run Status: %s | Primary Language: %s",
                    repository.getCloneGitStatus(),
                    repository.getRunGitStatus(),
                    repository.getPrimaryLanguage() != null ? repository.getPrimaryLanguage() : "Unknown"
            );
        } catch (GithubNotFoundException e) {
            log.error("Repository not found: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching status for {}: {}", repositoryId, e.getMessage(), e);
            throw new GithubProcessingException("Failed to fetch repository status");
        }
    }

    private String getPrimaryLanguage(String language) {
        try {
            Map<String, Long> languages = objectMapper.readValue(language, Map.class);
            return languages.keySet().stream()
                    .max((lang1, lang2) -> Long.compare(languages.get(lang1), languages.get(lang2)))
                    .orElse("Unknown");
        } catch (Exception e) {
            log.error("Failed to parse primary language: {}", e.getMessage(), e);
            return "Unknown";
        }
    }

    private String runProjectInDocker(File repoDir, String primaryLanguage) {
        String containerId = null;
        try {
            String imageName = getDockerImage(primaryLanguage);
            String[] command = getRunCommand(repoDir, primaryLanguage);
            if (command == null) {
                return "UNSUPPORTED";
            }
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(new HostConfig().withBinds(new Bind(repoDir.getAbsolutePath(), new Volume("/app"))))
                    .withWorkingDir("/app")
                    .withCmd(command);
            containerId = containerCmd.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();
            dockerClient.waitContainerCmd(containerId)
                    .exec(new com.github.dockerjava.core.command.WaitContainerResultCallback())
                    .awaitStatusCode(30, TimeUnit.SECONDS);
            return "SUCCESS";
        } catch (Exception e) {
            log.error("Failed to run project in {}: {}", repoDir.getAbsolutePath(), e.getMessage(), e);
            return "FAILED";
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.warn("Failed to cleanup container {}: {}", containerId, e.getMessage());
                }
            }
        }
    }

    private String[] getRunCommand(File repoDir, String primaryLanguage) throws IOException {
        switch (primaryLanguage.toLowerCase()) {
            case "javascript":
            case "typescript":
                String packageJsonPath = repoDir.getAbsolutePath() + "/package.json";
                if (Files.exists(Paths.get(packageJsonPath))) {
                    Map<String, Object> packageJson = objectMapper.readValue(new File(packageJsonPath), Map.class);
                    Map<String, String> scripts = (Map<String, String>) packageJson.get("scripts");
                    if (scripts != null && scripts.containsKey("start")) {
                        return new String[]{"npm", "install", "&&", "npm", "run", "start"};
                    }
                }
                return null;
            case "java":
                String pomPath = repoDir.getAbsolutePath() + "/pom.xml";
                if (Files.exists(Paths.get(pomPath))) {
                    return new String[]{"mvn", "clean", "install", "-DskipTests", "&&", "mvn", "spring-boot:run"};
                }
                return null;
            case "python":
                String requirementsPath = repoDir.getAbsolutePath() + "/requirements.txt";
                String[] baseCmd = Files.exists(Paths.get(requirementsPath))
                        ? new String[]{"pip", "install", "-r", "requirements.txt", "&&"}
                        : new String[]{};
                String mainPyPath = repoDir.getAbsolutePath() + "/main.py";
                String appPyPath = repoDir.getAbsolutePath() + "/app.py";
                if (Files.exists(Paths.get(mainPyPath))) {
                    return concatArrays(baseCmd, new String[]{"python", "main.py"});
                } else if (Files.exists(Paths.get(appPyPath))) {
                    return concatArrays(baseCmd, new String[]{"python", "app.py"});
                }
                return null;
            default:
                return null;
        }
    }

    private String[] concatArrays(String[] arr1, String[] arr2) {
        String[] result = new String[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
        return result;
    }

    private String getDockerImage(String primaryLanguage) {
        return switch (primaryLanguage.toLowerCase()) {
            case "javascript", "typescript" -> jsDockerImage;
            case "java" -> javaDockerImage;
            case "python" -> pythonDockerImage;
            default -> "ubuntu:latest";
        };
    }

    private void updateRepositoryStatus(Github repository) {
        repository.setCloneGitStatus(GitStatus.FAILED);
        repository.setRunGitStatus(GitStatus.FAILED);
        repository.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        githubRepository.save(repository);
    }

    private void cleanupCloneDirectory(String clonePath) {
        try {
            Files.walk(Paths.get(clonePath))
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to cleanup clone directory {}: {}", clonePath, e.getMessage());
        }
    }
}