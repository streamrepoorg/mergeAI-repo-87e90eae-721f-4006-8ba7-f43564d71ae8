package com.backend.repository.mail;

import com.backend.model.email.MagicLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MagicLinkRepository extends MongoRepository<MagicLink, String> {
    Optional<MagicLink> findByLink(String link);
}