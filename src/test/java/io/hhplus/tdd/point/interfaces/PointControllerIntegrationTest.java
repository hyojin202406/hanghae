package io.hhplus.tdd.point.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.domain.PointService;
import org.junit.jupiter.api.BeforeEach;
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
    private PointService pointService;

    private static final long USER_ID = 1L;
    private static final long INITIAL_CHARGE_AMOUNT = 100L;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        // 각 테스트에서 포인트를 초기화
        pointService.charge(USER_ID, INITIAL_CHARGE_AMOUNT);
    }

    @Test
    void 특정_유저의_포인트_조회_기능() throws Exception {
        final long id = USER_ID;

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    // 특정 유저가 포인트를 충전한 후, 해당 충전 내역이 제대로 저장되고,
    // API를 통해 올바르게 조회될 수 있는지 확인하기 위해 작성되었습니다.
    @Test
    void 특정_유저의_포인트_충전_이용_내역_조회_성공() throws Exception {
        final long id = USER_ID;

        // 충전 후에 내역을 확인하기 위해, 추가 충전을 하거나 사용 내역을 만듭니다.
        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(50)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").isNotEmpty());
    }

    @Test
    void 특정_유저의_포인트_충전_기능_성공() throws Exception {
        final long id = USER_ID;
        final long amount = 500;  // 추가로 500 포인트 충전

        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").isNumber())
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

    @Test
    void 특정_유저의_포인트_사용_기능_성공() throws Exception {
        final long id = USER_ID;
        final long amount = 50L;  // 50 포인트 사용

        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").isNumber())
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

}

