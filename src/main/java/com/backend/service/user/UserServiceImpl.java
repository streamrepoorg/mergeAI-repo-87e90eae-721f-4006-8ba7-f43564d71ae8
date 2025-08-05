package com.backend.service.user;

import com.backend.shared.exception.UserNotFoundException;
import com.backend.dto.UserDTO;
import com.backend.model.user.User;
import com.backend.repository.user.UserRepository;
import com.backend.config.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void updateUser(String email, UserDTO updatedInfo) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        User user = existingUser.get();
        if (updatedInfo.getUsername() != null && !updatedInfo.getUsername().isBlank()) {
            user.setUsername(updatedInfo.getUsername());
        }
        if (updatedInfo.getEmail() != null && !updatedInfo.getEmail().isBlank()) {
            user.setEmail(updatedInfo.getEmail());
        }
        if (updatedInfo.getPassword() != null && !updatedInfo.getPassword().isBlank()) {
            user.setPassword(PasswordUtil.encryptPassword(updatedInfo.getPassword()));
        }
        if (updatedInfo.getBio() != null && !updatedInfo.getBio().isBlank()) {
            user.setBio(updatedInfo.getBio());
        }
        if (updatedInfo.getName() != null && !updatedInfo.getName().isBlank()) {
            user.setName(updatedInfo.getName());
        }
        if (updatedInfo.getPicture() != null && !updatedInfo.getPicture().isBlank()) {
            user.setPicture(updatedInfo.getPicture());
        }
        userRepository.save(user);
    }
}
