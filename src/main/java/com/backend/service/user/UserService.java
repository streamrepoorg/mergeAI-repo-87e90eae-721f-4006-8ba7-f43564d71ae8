package com.backend.service.user;

import com.backend.dto.UserDTO;

public interface UserService {
    void updateUser(String email, UserDTO updatedInfo);
}
