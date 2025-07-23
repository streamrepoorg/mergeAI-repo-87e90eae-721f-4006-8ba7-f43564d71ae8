package com.backend.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String name;
    private String email;
    private String bio;
    private String picture; // Base64 string or URL
    private String password; // Hashed for manual registration
    private String provider; // "google", "github", or "manual"
    private String providerId; // OAuth2 provider ID
}