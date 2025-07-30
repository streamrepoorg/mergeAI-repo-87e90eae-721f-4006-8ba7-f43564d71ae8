package com.backend.controller.auth;

import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
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
        String validMagicLink = "pdZHp_KvX3n8pdrx_16FiYB9lFawILZkVfnE_WsWU_A";
        mockMvc.perform(get("/api/auth/validate-magic-link")
                        .param("link", validMagicLink))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your account has been verified successfully"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
//        Login token eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJva29yb2Fmb3JrZWxlY2hpMTIzQGdtYWlsLmNvbSIsInJvbGV
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

//    @Test
//    public void testOAuth2Success() throws Exception {
//        // Prepare OAuth2 user attributes
//        Map<String, Object> attributes = new HashMap<>();
//        attributes.put("sub", "123456789");
//        attributes.put("email", "okoroaforkelechi123@gmail.com");
//        attributes.put("name", "Kelechi Divine");
//        attributes.put("picture", "https://example.com/picture.jpg");
//
//        OAuth2User oAuth2User = new DefaultOAuth2User(null, attributes, "sub");
//        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
//                oAuth2User, null, "github");
//
//        when(authService.handleOAuth2User(
//                eq("github"),
//                eq("123456789"),
//                eq("okoroaforkelechi123@gmail.com"),
//                eq("Kelechi Divine"),
//                eq("https://example.com/picture.jpg")
//        )).thenReturn(userDTO);
//
//        when(jwtTokenProvider.generateToken(any())).thenReturn("mocked-jwt-token");
//
//        // Perform the request with mocked OAuth2 authentication
//        mockMvc.perform(get("/api/auth/oauth2/success")
//                        .with((RequestPostProcessor) authentication(authToken)))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("https://stream-repo-frontend.vercel.app/auth/callback?token=mocked-jwt-token"));
//    }
}