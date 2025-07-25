package com.backend.controller.auth;

import com.backend.BackendApplication;
import com.backend.config.exception.UserNotFoundException;
import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
import com.backend.model.user.User;
import com.backend.repository.user.UserRepository;
import com.backend.service.auth.AuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendApplication.class)
@Slf4j
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private AuthServiceImpl authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void test_createUserAccount() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("zipDemon");
        userDTO.setBio("Everybody's friendly neighbour");
        userDTO.setName("Kelechi Divine");
        userDTO.setPicture("");
        userDTO.setEmail("okoroaforkelechi123@gmail.com");
        userDTO.setPassword("StrongPassword128njowqe20i@#3@");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated());

        Optional<User> savedUser = userRepository.findByEmail("okoroaforkelechi123@gmail.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("zipDemon");
        log.info("Verified user in MongoDB: {}", savedUser.get());
    }

    @Test
    public void test_registerEmailAlreadyExists() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("zipDemon");
        userDTO.setBio("Everybody's friendly neighbour");
        userDTO.setName("Kelechi Divine");
        userDTO.setPicture("");
        userDTO.setEmail("okoroaforkelechi123@gmail.com");
        userDTO.setPassword("StrongPassword128njowqe20i@#3@");

        when(authService.existByEmail(anyString())).thenReturn(true);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with that email already exists"))
                .andExpect(jsonPath("$.status").value("CONFLICT"));
        verify(authService, times(1)).existByEmail("okoroaforkelechi123@gmail.com");
        verify(authService, never()).registerUser(any(UserDTO.class));
    }

    @Test
    public void test_registerInvalidFields() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("");
        userDTO.setBio("");
        userDTO.setName("");
        userDTO.setPicture("");
        userDTO.setEmail("okoroaforkelechi123@gmail.com");
        userDTO.setPassword("StrongPassword128njowqe20i@#3@");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username must be valid"))
                .andExpect(jsonPath("$.email").value("Email must be valid"))
                .andExpect(jsonPath("$.password").value("Password must be at least 8 characters"));

        verify(authService, never()).existByEmail(anyString());
        verify(authService, never()).registerUser(any(UserDTO.class));
    }

//    @Test
//    public void testLogin_Success() throws Exception {
//        UserDTO userDTO = new UserDTO();
//        userDTO.setUsername("testuser");
//        userDTO.setPassword("securePass123");
//
//        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "securePass123", Collections.emptyList());
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
//        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userDTO)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("jwt-token"))
//                .andExpect(jsonPath("$.message").value("Login successful"))
//                .andExpect(jsonPath("$.status").value("OK"));
//
//        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(jwtTokenProvider, times(1)).generateToken(authentication);
//    }

//    @Test
//    public void testLogin_InvalidCredentials() throws Exception {
//        // Arrange
//        UserDTO userDTO = new UserDTO();
//        userDTO.setUsername("testuser");
//        userDTO.setPassword("wrongPass");
//
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});
//
//        // Act & Assert
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userDTO)))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.message").value("Invalid credentials"))
//                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
//
//        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
//    }
//
    @Test
    public void test_requestMagicLinkSuccess() throws Exception {
        MagicLinkRequest request = new MagicLinkRequest();
        request.setEmail("okoroaforkelechi123@gmail.com");
        doNothing().when(authService).requestMagicLink(anyString());
        mockMvc.perform(post("/api/auth/magic-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Magic link sent successfully"))
                .andExpect(jsonPath("$.status").value("OK"));
        verify(authService, times(1)).requestMagicLink("okoroaforkelechi123@gmail.com");
    }
}