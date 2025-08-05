package com.backend.model.github;

import com.backend.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Github {

        @Id
        private String id;
        private User user;
        private String githubLink;
        private String resultUrl;
        private GitStatus cloneGitStatus;

        @Field("languages")
        private String primaryLanguage;
        private GitStatus runGitStatus;
        private Instant createdAt;
        private Instant updatedAt;
}
