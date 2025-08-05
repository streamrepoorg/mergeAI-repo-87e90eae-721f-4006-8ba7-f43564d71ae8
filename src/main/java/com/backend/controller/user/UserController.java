package com.backend.controller.user;

import com.backend.shared.exception.UserNotFoundException;
import com.backend.dto.UserDTO;
import com.backend.dto.response.ResponseDetails;
import com.backend.service.user.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserDTO updatedUser) {
        try {
            if (updatedUser.getEmail() == null || updatedUser.getEmail().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDetails(LocalDateTime.now(), "Email is required for update", HttpStatus.BAD_REQUEST.toString(), "/api/user/update"));
            }
            log.info("Updating user with email: {}", updatedUser.getEmail());
            userService.updateUser(updatedUser.getEmail(), updatedUser);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "User info updated successfully", HttpStatus.OK.toString(), "/api/user/update");
            return ResponseEntity.status(200).body(responseDetails);

        } catch (UserNotFoundException e) {
            log.error("Update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDetails(LocalDateTime.now(), e.getMessage(), HttpStatus.NOT_FOUND.toString(), "/api/user/update"));
        } catch (Exception e) {
            log.error("Unexpected error during update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseDetails(LocalDateTime.now(), "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR.toString(), "/api/user/update"));
        }
    }
}
