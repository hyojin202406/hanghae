package io.hhplus.tdd.point.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.exception.InvalidUserIdException;
import io.hhplus.tdd.point.exception.PointValidationException;
import io.hhplus.tdd.point.exception.UserPointNotFoundException;
import io.hhplus.tdd.point.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static io.hhplus.tdd.point.domain.TransactionType.CHARGE;
import static io.hhplus.tdd.point.domain.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
            verify(pointRepository, never()).getUserPoint(anyLong());
        }

        @DisplayName("정상적인 유저 아이디로 포인트 조회")
        @Test
        void validUserId() throws Exception {
            // Given
            long validId = 1L;
            UserPoint mockUserPoint = new UserPoint(validId, 0, System.currentTimeMillis());
            when(pointRepository.getUserPoint(validId)).thenReturn(mockUserPoint);

            // When
            UserPoint result = pointService.getUserPoint(validId);

            // Then
            assertNotNull(result);
            assertEquals(mockUserPoint.id(), result.id());
            assertEquals(mockUserPoint.point(), result.point());
            verify(pointRepository, times(1)).getUserPoint(validId);
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
            verify(pointRepository, never()).getUserHistory(anyLong());
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
            verify(pointRepository, times(1)).getUserHistory(validId);
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
            verify(pointRepository, never()).getUserHistory(anyLong());
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
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @DisplayName("[charge] 유저 포인트가 유효하지 않을 경우 예외 처리")
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

    @Nested
    @DisplayName("[use] 유저 포인트 사용 기능 테스트")
    class UseTest {
        @DisplayName("[use] 유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() throws Exception {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.getUserPoint(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @DisplayName("[use] 유저 포인트가 유효하지 않을 경우 예외 처리")
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

        @DisplayName("[use] 유저 포인트 조회 실패시 예외 처리")
        @Test
        void userPointNotFound() throws Exception {
            // Given
            long validId = 1L;

            // 유저 포인트 조회 시 null 반환을 모킹
            when(pointRepository.getUserPoint(validId)).thenReturn(null);

            // When & Then
            UserPointNotFoundException exception = assertThrows(UserPointNotFoundException.class, () -> {
                pointService.useUserPoint(validId, 1000L);
            });

            assertEquals("Failed to find user point for ID: " + validId, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @DisplayName("[use] 남은 포인트가 부족할 경우 예외 처리")
        @Test
        void insufficientUserPoint() throws Exception {
            // Given
            long validId = 1L;
            long requestAmount = 1000L;

            // 유저의 현재 포인트는 500으로 설정
            UserPoint currentUserPoint = new UserPoint(validId, 500L, System.currentTimeMillis());
            when(pointRepository.getUserPoint(validId)).thenReturn(currentUserPoint);

            // When & Then
            PointValidationException exception = assertThrows(PointValidationException.class, () -> {
                pointService.useUserPoint(validId, requestAmount);
            });

            assertEquals("Point validation exception. remainingBalance : " + (500L - requestAmount), exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @DisplayName("[use] 유저 포인트 사용 성공")
        @Test
        void useUserPointSuccess() throws Exception {
            // Given
            long validId = 1L;
            long currentAmount = 2000L;
            long useAmount = 1000L;
            long updatedAmount = currentAmount - useAmount;

            // 유저의 현재 포인트 설정
            UserPoint currentUserPoint = new UserPoint(validId, currentAmount, System.currentTimeMillis());
            when(pointRepository.getUserPoint(validId)).thenReturn(currentUserPoint);

            // 포인트 사용 후 업데이트된 포인트 설정
            UserPoint updatedUserPoint = new UserPoint(validId, updatedAmount, System.currentTimeMillis());
            when(pointRepository.insertOrUpdate(validId, updatedAmount)).thenReturn(updatedUserPoint);

            // When
            UserPoint result = pointService.useUserPoint(validId, useAmount);

            // Then
            assertEquals(updatedAmount, result.point());
            verify(pointRepository).insertOrUpdate(validId, updatedAmount);
            verify(pointRepository).insertHistory(eq(validId), eq(useAmount), eq(TransactionType.USE), anyLong());
        }
    }


}
