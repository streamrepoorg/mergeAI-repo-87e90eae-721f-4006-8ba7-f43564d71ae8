package com.backend.controller.user;

import com.backend.dto.UserDTO;
import com.backend.service.user.UserServiceImpl;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Slf4j
public class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserDTO userDTO;

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
    public void testUpdateUserSuccess() throws Exception {
        userDTO.setBio("Updated bio: Your friendly neighbour");
        userDTO.setName("Kelechi Divine Updated");
        mockMvc.perform(put("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User info updated successfully"));
    }
}