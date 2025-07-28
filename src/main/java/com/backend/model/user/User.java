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
    private String picture;
    private String password;
    private String provider;
    private Role role = Role.USER;
    private Boolean isVerified = false;
    private String providerId;
}