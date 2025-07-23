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
        userDTO.setUsername("ziemon");
        userDTO.setBio("Everybody's friendly neighbour");
        userDTO.setName("Kelechi Divine");
        userDTO.setPicture("");
        userDTO.setEmail("okorkelechi123@gmail.com");
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
    public void testRegister_EmailExists() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");

        when(authService.existByEmail(anyString())).thenReturn(true);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with this email already exists"))
                .andExpect(jsonPath("$.status").value("CONFLICT"));
        verify(authService, times(1)).existByEmail("test@example.com");
        verify(authService, never()).registerUser(any(UserDTO.class));
    }

    @Test
    public void testRegister_InvalidInput() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("");
        userDTO.setName("Test User");
        userDTO.setEmail("invalid-email");
        userDTO.setPassword("short");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username is required"))
                .andExpect(jsonPath("$.email").value("Email must be valid"))
                .andExpect(jsonPath("$.password").value("Password must be at least 8 characters"));

        verify(authService, never()).existByEmail(anyString());
        verify(authService, never()).registerUser(any(UserDTO.class));
    }

    @Test
    public void testLogin_Success() throws Exception {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setPassword("securePass123");

        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "securePass123", Collections.emptyList());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.status").value("OK"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateToken(authentication);
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setPassword("wrongPass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    public void testRequestMagicLink_Success() throws Exception {
        MagicLinkRequest request = new MagicLinkRequest();
        request.setEmail("test@example.com");
        doNothing().when(authService).requestMagicLink(anyString());
        mockMvc.perform(post("/api/auth/magic-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Magic link sent to email"))
                .andExpect(jsonPath("$.status").value("OK"));

        verify(authService, times(1)).requestMagicLink("test@example.com");
    }

    @Test
    public void testRequestMagicLink_UserNotFound() throws Exception {
        MagicLinkRequest request = new MagicLinkRequest();
        request.setEmail("invalid-email");
        doThrow(new UserNotFoundException("User not found")).when(authService).requestMagicLink(anyString());
        mockMvc.perform(post("/api/auth/magic-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verify(authService, times(1)).requestMagicLink("invalid-email");
    }

    @Test
    public void testMagicLogin_Success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");

        when(authService.validateMagicLink("valid-token")).thenReturn(userDTO);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        mockMvc.perform(get("/api/auth/magic-login")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Magic link login successful"))
                .andExpect(jsonPath("$.status").value("OK"));

        verify(authService, times(1)).validateMagicLink("valid-token");
        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    public void testMagicLogin_InvalidToken() throws Exception {
        when(authService.validateMagicLink("invalid-token"))
                .thenThrow(new RuntimeException("Invalid or expired magic link"));
        mockMvc.perform(get("/api/auth/magic-login")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired magic link"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verify(authService, times(1)).validateMagicLink("invalid-token");
    }

    @Test
    public void testOAuth2Success_Google() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "12345");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");
        attributes.put("picture", "base64-image");

        OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oAuth2User, Collections.emptyList(), "google");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setName("Test User");
        userDTO.setPicture("base64-image");

        when(authService.handleOAuth2User("google", "12345", "test@example.com", "Test User", "base64-image"))
                .thenReturn(userDTO);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        mockMvc.perform(get("/api/auth/oauth2/success")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("OAuth2 login successful"))
                .andExpect(jsonPath("$.status").value("OK"));

        verify(authService, times(1)).handleOAuth2User("google", "12345", "test@example.com", "Test User", "base64-image");
        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    public void testOAuth2Success_GitHub() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "67890");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");
        attributes.put("picture", "base64-image");

        OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oAuth2User, Collections.emptyList(), "github");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setName("Test User");
        userDTO.setPicture("base64-image");

        when(authService.handleOAuth2User("github", "67890", "test@example.com", "Test User", "base64-image"))
                .thenReturn(userDTO);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        mockMvc.perform(get("/api/auth/oauth2/success")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("OAuth2 login successful"))
                .andExpect(jsonPath("$.status").value("OK"));

        verify(authService, times(1)).handleOAuth2User("github", "67890", "test@example.com", "Test User", "base64-image");
        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }
}