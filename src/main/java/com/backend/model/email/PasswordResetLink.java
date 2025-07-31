package com.backend.model.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetLink {

    @Id
    private String userId;

    private String link;

    private Instant expiresAt;
}

