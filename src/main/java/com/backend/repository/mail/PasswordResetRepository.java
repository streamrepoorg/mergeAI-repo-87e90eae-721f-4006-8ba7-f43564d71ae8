package com.backend.repository.mail;

import com.backend.model.email.PasswordResetLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends MongoRepository<PasswordResetLink, String> {
    Optional<PasswordResetLink> findByLink (String link);
}