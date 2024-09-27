package io.hhplus.tdd.point.domain;

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
class PointServiceIntegrationV2Test {

    @Autowired
    PointService pointService;

    @Autowired
    PointRepository pointRepository;

    private static final long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        pointService.charge(USER_ID, 1000);
    }

    @Test
    void 동시에_충전과_사용_요청을_보내면_정상적으로_모두_처리된다() throws Exception {

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
