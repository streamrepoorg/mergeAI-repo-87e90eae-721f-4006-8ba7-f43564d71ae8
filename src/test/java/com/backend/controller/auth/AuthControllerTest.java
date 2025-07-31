package com.backend.controller.auth;

import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
import com.backend.dto.request.PasswordResetRequest;
import com.backend.service.auth.AuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Slf4j
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AuthServiceImpl authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserDTO userDTO;

    private JwtTokenProvider jwtTokenProvider;

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
    }

    @Test
    public void testCreateUserAccountSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your account has been created successfully"));
    }

    @Test
    public void testRequestMagicLinkSuccess() throws Exception {
        MagicLinkRequest magicLinkRequest = new MagicLinkRequest();
        magicLinkRequest.setEmail(userDTO.getEmail());
        mockMvc.perform(post("/api/auth/magic-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(magicLinkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Magic link sent to email"));
    }

    @Test
    public void testValidateMagicLinkSuccess() throws Exception {
        String validMagicLink = "7pPaTXs-T2Hy5taRpTrbbksq7AH-BETTYUUig6XL7zE";
        mockMvc.perform(get("/api/auth/validate-magic-link")
                        .param("link", validMagicLink))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your account has been verified successfully"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void testRequestPasswordResetSuccess() throws Exception {
        MagicLinkRequest passwordResetRequest = new MagicLinkRequest();
        passwordResetRequest.setEmail(userDTO.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordResetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset link sent to email"));
    }

    @Test
    public void testResetPasswordSuccess() throws Exception {
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setLink("zV4Ss-HaJwjXYmXXqFilX46so4vov7_nAZmYg9uaMAs");
        resetRequest.setNewPassword("NewStrongPassword128@#3@");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }
}