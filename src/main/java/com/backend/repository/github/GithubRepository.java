package com.backend.repository.github;

import com.backend.model.github.Github;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GithubRepository extends MongoRepository<Github, String> {}