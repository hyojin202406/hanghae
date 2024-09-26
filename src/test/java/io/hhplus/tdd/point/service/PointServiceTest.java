package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.*;
import io.hhplus.tdd.point.exception.InvalidUserIdException;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static io.hhplus.tdd.point.domain.TransactionType.CHARGE;
import static io.hhplus.tdd.point.domain.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private UserPoint userPoint;

    @InjectMocks
    PointService pointService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("[point] 유저 포인트 조회 테스트")
    class PointTest {
        @DisplayName("유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.point(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).point(anyLong());
        }

        @DisplayName("정상적인 유저 아이디로 포인트 조회")
        @Test
        void validUserId() {
            // Given
            long validId = 1L;
            UserPoint point = new UserPoint(validId, 0, System.currentTimeMillis());
            when(pointRepository.point(validId)).thenReturn(point);

            // When
            UserPoint result = pointService.point(validId);

            // Then
            assertNotNull(result);
            assertEquals(point.id(), result.id());
            assertEquals(point.point(), result.point());
            verify(pointRepository, times(1)).point(validId);
        }
    }


    @Nested
    @DisplayName("[history] 유저 포인트 충전/이용 내역 조회 테스트")
    class HistoryTest {
        @DisplayName("[history] 유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.point(invalidId);
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
            List<PointHistory> result = pointService.history(validId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(mockHistory.get(0).amount(), result.get(0).amount());
            verify(pointRepository, times(1)).getUserHistory(validId);
        }
    }

    @Nested
    @DisplayName("[charge] 유저 포인트 충전 기능 테스트")
    class ChargeTest {
        @DisplayName("[charge] 유저 아이디가 유효하지 않을 경우 예외 처리")
        @Test
        void invalidUserId() {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.point(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @Test
        void 유저_포인트가_유효하지_않을_경우_예외_처리() {
            // Given
            long validId = 1L;
            long invalidAmount = -50L; // 유효하지 않은 음수 충전 금액
            UserPoint initialPoint = new UserPoint(validId, 200, System.currentTimeMillis());

            // Mocking repository behavior
            when(pointRepository.point(validId)).thenReturn(initialPoint);

            // When / Then
            PointValidationException thrown = assertThrows(PointValidationException.class, () -> {
                pointService.charge(validId, invalidAmount);
            });

            assertEquals("Invalid charge amount: " + invalidAmount, thrown.getMessage());
            verify(pointRepository, times(1)).point(validId);
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointRepository, never()).insertHistory(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        void 유효한_아이디와_포인트일_경우_포인트_충전_성공() {
            // Given
            long validId = 1L;
            long amount = 100L;
            UserPoint initialPoint = new UserPoint(validId, 200, System.currentTimeMillis());
            UserPoint chargedPoint = new UserPoint(validId, 300, System.currentTimeMillis());

            // Mocking repository behaviors
            when(pointRepository.point(validId)).thenReturn(initialPoint);
            when(pointRepository.insertOrUpdate(validId, chargedPoint.point())).thenReturn(chargedPoint);

            // When
            UserPoint result = pointService.charge(validId, amount);

            // Then
            assertNotNull(result);
            assertEquals(validId, result.id());
            assertEquals(300, result.point()); // initialPoint의 포인트 200 + 충전 포인트 100
            verify(pointRepository, times(1)).point(validId);
            verify(pointRepository, times(1)).insertOrUpdate(validId, chargedPoint.point());
            verify(pointRepository, times(1)).insertHistory(validId, amount, TransactionType.CHARGE, result.updateMillis());
        }

        @Test
        void 충전_내역_저장_실패_시_예외_발생() {
            // Given
            long validId = 1L;
            long amount = 100L;
            UserPoint initialPoint = new UserPoint(validId, 100, System.currentTimeMillis());
            UserPoint chargedPoint = new UserPoint(validId, 200, System.currentTimeMillis());

            when(pointRepository.point(validId)).thenReturn(initialPoint);
            when(pointRepository.insertOrUpdate(validId, chargedPoint.point())).thenReturn(chargedPoint);
            doThrow(new RuntimeException("History insert failed")).when(pointRepository).insertHistory(validId, amount, TransactionType.CHARGE, chargedPoint.updateMillis());

            // When & Then
            Exception exception = assertThrows(RuntimeException.class, () -> pointService.charge(validId, amount));

            // Validate exception message
            assertEquals("History insert failed", exception.getMessage());

            // Verify interactions
            verify(pointRepository, times(1)).point(validId);
            verify(pointRepository, times(1)).insertOrUpdate(validId, chargedPoint.point());
            verify(pointRepository, times(1)).insertHistory(validId, amount, TransactionType.CHARGE, chargedPoint.updateMillis());
        }

    }

    @Nested
    @DisplayName("[use] 유저 포인트 사용 기능 테스트")
    class UseTest {
        @Test
        void 유저_아이디가_유효하지_않을_경우_예외_처리() {
            // Given
            long invalidId = -1L;

            // When & Then
            InvalidUserIdException exception = assertThrows(InvalidUserIdException.class, () -> {
                pointService.point(invalidId);
            });

            assertEquals("Invalid user ID: " + invalidId, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @Test
        void 유저_포인트가_유효하지_않을_경우_예외_처리() {
            // Given
            long validId = 1L;
            long invalidAmount = -1000L;

            // userPoint 객체를 미리 생성하고 반환되도록 설정
            UserPoint userPoint = mock(UserPoint.class);
            when(pointRepository.point(validId)).thenReturn(userPoint);

            // 포인트 충전 유효성 검증에 대한 Mock 설정
            doThrow(new PointValidationException("Point validation exception. amount: " + invalidAmount))
                    .when(userPoint).use(invalidAmount);

            // When & Then
            PointValidationException exception = assertThrows(PointValidationException.class, () -> {
                pointService.use(validId, invalidAmount);
            });

            // 예외 메시지 검증
            assertEquals("Point validation exception. amount: " + invalidAmount, exception.getMessage());

            // pointRepository의 insertOrUpdate 메서드가 호출되지 않았는지 검증
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @Test
        void 남은_포인트가_부족할_경우_예외_처리() {
            // Given
            long validId = 1L;
            long requestAmount = 1000L;
            long remainingPoint = 500L - 1000L;

            // 유저의 현재 포인트는 500으로 설정
            UserPoint currentUserPoint = new UserPoint(validId, 500L, System.currentTimeMillis());
            when(pointRepository.point(validId)).thenReturn(currentUserPoint);

            // 포인트 충전 유효성 검증에 대한 Mock 설정
            doThrow(new PointValidationException("Insufficient balance: attempted to use: " + requestAmount + ", remaining balance: " + remainingPoint))
                    .when(userPoint).use(requestAmount);

            // When & Then
            PointValidationException exception = assertThrows(PointValidationException.class, () -> {
                pointService.use(validId, requestAmount);
            });

            assertEquals("Insufficient balance: attempted to use: " + requestAmount + ", remaining balance: " + remainingPoint, exception.getMessage());
            verify(pointRepository, never()).insertOrUpdate(anyLong(), anyLong());
        }

        @Test
        void 유저_포인트_사용_성공() {
            // Given
            UserPoint point = new UserPoint(1L, 1000L, System.currentTimeMillis());
            when(pointRepository.point(1L)).thenReturn(point);
            UserPoint usedPoint = new UserPoint(1L, 300L, System.currentTimeMillis());
            when(userPoint.use(700L)).thenReturn(usedPoint);
            when(pointRepository.insertOrUpdate(1L, usedPoint.point())).thenReturn(usedPoint);

            // When
            UserPoint result = pointService.use(1L, 700L);

            // Then
            assertNotNull(result);  // 결과가 null이 아님을 확인
            assertEquals(300L, result.point());  // 포인트가 업데이트되었는지 확인
        }
    }
}
