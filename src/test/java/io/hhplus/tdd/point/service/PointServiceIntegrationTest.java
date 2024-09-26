package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointService;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    PointService pointService;

    private static final long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        pointService.charge(USER_ID, 1000);
    }

    @Test
    void 동시성_제어_테스트_포인트_충전과_포인트_사용을_성공() throws Exception {

        final int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i -> {
            executorService.execute(() -> {
                if (i % 2 == 0) {
                    pointService.charge(USER_ID, 10);
                    System.out.println("Charged: 10, Current Point: " + pointService.point(USER_ID).point());
                } else {
                    pointService.use(USER_ID, 5);
                    System.out.println("Used: 5, Current Point: " + pointService.point(USER_ID).point());
                }
                latch.countDown();
            });
        });

        latch.await();

        long expectedPoint = 1250;
        UserPoint point = pointService.point(USER_ID);

        assertEquals(expectedPoint, point.point());
    }

}
