package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryConfig {
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}
