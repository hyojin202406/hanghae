package io.hhplus.tdd.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    PointService pointService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        pointService.chargeUserPoint(1L, 1000);
        pointService.useUserPoint(1L, 100);
    }

    @DisplayName("특정 유저의 포인트 조회 기능")
    @Test
    void point() throws Exception {
        // Given
        final long id = 1L;

        // When & Then
        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(900L))
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

    @DisplayName("특정 유저의 포인트 충전/이용 내역 조회")
    @Test
    void history() throws Exception {
        // Given
        final long id = 1L;

        // When & Then
        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(id))
                .andExpect(jsonPath("$[0].amount").value(1000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].updateMillis").isNumber())
                .andExpect(jsonPath("$[1].userId").value(id))
                .andExpect(jsonPath("$[1].amount").value(100L))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].updateMillis").isNumber());
    }

    @DisplayName("특정 유저의 포인트 충전 기능")
    @Test
    void charge() throws Exception {
        final long id = 1L;
        final long amount = 500;  // 추가로 500 포인트 충전

        // When & Then
        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(500))
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

    @DisplayName("특정 유저의 포인트 사용 기능")
    @Test
    void use() throws Exception {
        final long id = 1L;
        final long amount = 400;  // 400 포인트 사용

        // When & Then
        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(500))  // 기존 900 - 400 = 500
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

}