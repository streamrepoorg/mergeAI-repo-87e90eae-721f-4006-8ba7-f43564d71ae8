package com.backend.controller.auth;

import com.backend.config.exception.PasswordOrEmailException;
import com.backend.config.exception.UserAlreadyExistException;
import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
import com.backend.service.auth.AuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
@Slf4j
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
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserDTO userDTO;
    private MagicLinkRequest magicLinkRequest;
    private OAuth2AuthenticationToken oauth2Token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userDTO = new UserDTO();
        userDTO.setUsername("zipDemon");
        userDTO.setBio("Everybody's friendly neighbour");
        userDTO.setName("Kelechi Divine");
        userDTO.setPicture("");
        userDTO.setEmail("okoroaforkelechi123@gmail.com");
        userDTO.setPassword("StrongPassword128njowqe20i@#3@");

        magicLinkRequest = new MagicLinkRequest();
        magicLinkRequest.setEmail("okoroaforkelechi123@gmail.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456");
        attributes.put("email", "okoroaforkelechi123@gmail.com");
        attributes.put("name", "Kelechi Divine");
        attributes.put("picture", "https://github.com/OkoroaforKelechiDivine/avatar.png");
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "sub"
        );
        oauth2Token = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                "github"
        );
    }

    @Test
    void testCreateUserAccountSuccess() throws Exception {
        doNothing().when(authService).registerUser(any(UserDTO.class));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your account has been created successfully"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(authService, times(1)).registerUser(any(UserDTO.class));
    }

    @Test
    void testCreateUserAccountEmailAlreadyExists() throws Exception {
        doThrow(new UserAlreadyExistException("Email already exists")).when(authService).registerUser(any(UserDTO.class));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verify(authService, times(1)).registerUser(any(UserDTO.class));
    }

    @Test
    void testCreateUserAccountEmptyFields() throws Exception {
        UserDTO invalidUserDTO = new UserDTO();
        invalidUserDTO.setUsername("");
        invalidUserDTO.setEmail("");
        invalidUserDTO.setPassword("");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username cannot be empty"));

        verify(authService, never()).registerUser(any(UserDTO.class));
    }

    @Test
    void testCreateUserAccountWeakPassword() throws Exception {
        userDTO.setPassword("weak");
        doThrow(new PasswordOrEmailException("Password is too weak.", new Throwable("Invalid password strength"))).when(authService).registerUser(any(UserDTO.class));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is too weak."))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verify(authService, times(1)).registerUser(any(UserDTO.class));
    }

    @Test
    void testLoginSuccess() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDTO.getUsername(),
                userDTO.getPassword(),
                Collections.emptyList()
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.responseDetails.message").value("Login successful"))
                .andExpect(jsonPath("$.responseDetails.status").value("OK"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new RuntimeException("Invalid credentials"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateToken(any(Authentication.class));
    }
}