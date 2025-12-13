package ru.practicum.ewm.controller.publicapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PublicEventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEvents_WithoutParams_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_WithFromAndSize_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_WithInvalidFrom_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithInvalidSize_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}