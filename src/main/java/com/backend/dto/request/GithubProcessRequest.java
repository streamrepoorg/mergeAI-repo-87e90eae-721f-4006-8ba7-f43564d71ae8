package com.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GithubProcessRequest {
    @NotBlank(message = "GitHub link is required")
    private String githubLink;
}
