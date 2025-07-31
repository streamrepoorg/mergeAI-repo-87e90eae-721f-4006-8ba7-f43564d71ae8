package com.backend.service.auth;

import com.backend.dto.UserDTO;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface AuthService {
    void registerUser(UserDTO userDTO);
    UserDTO handleOAuth2User(String provider, String providerId, String email, String name, String picture);
    void requestMagicLink(String email);
    void validateMagicLink(String link);
    UserDTO handleOAuth2Redirect(OAuth2AuthenticationToken authentication); // New method
}