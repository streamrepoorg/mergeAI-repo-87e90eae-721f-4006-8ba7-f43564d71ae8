package com.backend.controller.waitingList;

import com.backend.dto.UserDTO;
import com.backend.dto.request.WaitingListDTO;
import com.backend.service.waitinglist.WaitingListServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Slf4j
public class WaitingListControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WaitingListServiceImpl waitingListService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private WaitingListDTO waitingListDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        waitingListDTO = new WaitingListDTO();
        waitingListDTO.setEmail("okoroaforkelechi123@gmail.com");
        waitingListDTO.setName("Kelechi Divine");
    }

    @Test
    public void testCreateWaitingList() throws Exception {
        mockMvc.perform(post("/waiting-list/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(waitingListDTO)))
                .andExpect(status().isCreated());
    }
}