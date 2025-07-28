package com.backend.model.email;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "magic_links")
public class MagicLink {
    @Id
    private String id;
    private String userId;
    private String link;
    private Instant expiresAt;
}