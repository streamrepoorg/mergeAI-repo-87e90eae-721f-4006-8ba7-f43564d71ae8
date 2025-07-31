package com.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @NotBlank(message = "Link cannot be empty")
    private String link;

    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
}