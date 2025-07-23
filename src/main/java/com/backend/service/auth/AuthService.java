package com.backend.service.auth;

import com.backend.dto.UserDTO;
import com.backend.model.user.User;

public interface AuthService {
    User registerUser(UserDTO userDTO);
    User handleOAuth2User(String provider, String providerId, String email, String name, String picture);
    void requestMagicLink(String email);
    User validateMagicLink(String token);
}