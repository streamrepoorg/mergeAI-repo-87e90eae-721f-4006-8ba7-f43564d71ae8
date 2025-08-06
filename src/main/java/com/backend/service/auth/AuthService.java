package com.backend.service.auth;

import com.backend.dto.UserDTO;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface AuthService {
    void registerUser(UserDTO userDTO);
    UserDTO handleOAuth2User(String provider, String providerId, String email, String name, String picture);
    UserDTO handleOAuth2Redirect(OAuth2AuthenticationToken authentication);
    void requestMagicLink(String email);
    void validateMagicLink(String link);
    void requestPasswordReset(String email);
    void resetPassword(String link, String newPassword);
    String handleOAuth2GithubRedirect(OAuth2AuthenticationToken authentication); // New method
}