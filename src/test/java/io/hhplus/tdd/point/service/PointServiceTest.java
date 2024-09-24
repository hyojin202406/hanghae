package io.hhplus.tdd.point.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.exception.InvalidUserIdException;
import io.hhplus.tdd.point.exception.PointValidationException;
import io.hhplus.tdd.point.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static io.hhplus.tdd.point.domain.TransactionType.CHARGE;
import static io.hhplus.tdd.point.domain.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointServiceTest.class)
class PointServiceTest {

    @Autowired
    MockMvc mockMvc;

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    PointService pointService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("[point] 유저 포인트 조회 테스트")
    class PointTest {
        @DisplayName("유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() throws Exception {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.getUserPoint(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).getUserPoint(anyLong()); //getUserPoint가 호출되지 않아야 함
        }

        @DisplayName("정상적인 유저 아이디로 포인트 조회")
        @Test
        void validUserId() throws Exception {
            // Given
            long validId = 1L;
            UserPoint mockUserPoint = new UserPoint(validId, 0, System.currentTimeMillis()); // 필요한 필드 설정
            when(pointRepository.getUserPoint(validId)).thenReturn(mockUserPoint);

            // When
            UserPoint result = pointService.getUserPoint(validId);

            // Then
            assertNotNull(result);
            assertEquals(mockUserPoint.id(), result.id());
            assertEquals(mockUserPoint.point(), result.point());
            verify(pointRepository, times(1)).getUserPoint(validId); // getUserPoint가 한번 호출되어야 함
        }
    }


    @Nested
    @DisplayName("[history] 유저 포인트 충전/이용 내역 조회 테스트")
    class HistoryTest {
        @DisplayName("[history] 유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() throws Exception {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.getUserPoint(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).getUserHistory(anyLong()); // getUserHistory가 호출되지 않아야 함
        }

        @DisplayName("[history] 유효한 아이디일 경우 포인트 충전/이용 내역 조회")
        @Test
        void validUserHistories() {
            // Given
            long validId = 1L;
            List<PointHistory> mockHistory = Arrays.asList(
                    new PointHistory(1L, validId, 100, CHARGE, System.currentTimeMillis()),
                    new PointHistory(2L, validId, 50, USE, System.currentTimeMillis())
            );
            when(pointRepository.getUserHistory(validId)).thenReturn(mockHistory);

            // When
            List<PointHistory> result = pointService.getUserHistory(validId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(mockHistory.get(0).amount(), result.get(0).amount());
            verify(pointRepository, times(1)).getUserHistory(validId); // getUserHistory가 한번 호출되어야 함
        }
    }

    @Nested
    @DisplayName("[chargeUserPoint] 유저 포인트 충전/이용 내역 조회 테스트")
    class ChargeUserPointTest {
        @Test
        void invalidUserId() throws Exception {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.getUserPoint(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).getUserHistory(anyLong()); // getUserHistory가 호출되지 않아야 함
        }
    }

    @Nested
    @DisplayName("[charge] 유저 포인트 충전 기능 테스트")
    class ChargeTest {
        @DisplayName("[charge] 유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() throws Exception {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.getUserPoint(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong()); // insertOrUpdate가 호출되지 않아야 함
        }

        @DisplayName("[charge] 포인트가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserPoint() throws Exception {
            // Givne
            long validId = 1L;
            long invalidAmount = -1000L;

            // When & Then
            PointValidationException exception = assertThrows(PointValidationException.class, () -> {
                pointService.chargeUserPoint(validId, invalidAmount);
            });

            assertEquals("Point validation exception. amount : " + invalidAmount, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @DisplayName("[charge] 유효한 아이디와 포인트일 경우 포인트 충전 성공")
        @Test
        void chargeUserPointSuccess() throws Exception {
            // Given
            long validId = 1L;
            long validAmount = 1000L;
            UserPoint mockUserPoint = new UserPoint(validId, validAmount, System.currentTimeMillis());

            when(pointRepository.insertOrUpdate(validId, validAmount)).thenReturn(mockUserPoint);

            // When
            UserPoint result = pointService.chargeUserPoint(validId, validAmount);

            // Then
            assertNotNull(result);
            assertEquals(mockUserPoint.id(), result.id());
            assertEquals(mockUserPoint.point(), result.point());

            verify(pointRepository, times(1)).insertOrUpdate(validId, validAmount);
            verify(pointRepository, times(1)).insertHistory(eq(validId), eq(validAmount), eq(TransactionType.CHARGE), anyLong());
        }

        @DisplayName("[charge] 충전 내역 저장 실패 시 예외 발생")
        @Test
        void chargeUserPointHistoryFailure() throws Exception {
            // Given
            long validId = 1L;
            long validAmount = 1000L;
            UserPoint mockUserPoint = new UserPoint(validId, validAmount, System.currentTimeMillis());

            when(pointRepository.insertOrUpdate(validId, validAmount)).thenReturn(mockUserPoint);
            doThrow(new RuntimeException("History insert failed")).when(pointRepository).insertHistory(anyLong(), anyLong(), any(TransactionType.class), anyLong());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                pointService.chargeUserPoint(validId, validAmount);
            });

            assertEquals("History insert failed", exception.getMessage());
            verify(pointRepository, times(1)).insertOrUpdate(validId, validAmount);
            verify(pointRepository, times(1)).insertHistory(eq(validId), eq(validAmount), eq(TransactionType.CHARGE), anyLong());
        }

    }


}
