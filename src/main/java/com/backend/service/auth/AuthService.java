package com.backend.service.auth;

import com.backend.dto.UserDTO;
import com.backend.model.user.User;

public interface AuthService {
    void registerUser(UserDTO userDTO);
    UserDTO handleOAuth2User(String provider, String providerId, String email, String name, String picture);
    void requestMagicLink(String email);
    String validateMagicLink(String token);
}