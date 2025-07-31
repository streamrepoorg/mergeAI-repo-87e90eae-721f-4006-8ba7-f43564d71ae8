package com.backend.service.email;

public interface EmailService {
    void sendMagicLink(String email, String link);
    void sendPasswordResetLink(String email, String link);
}