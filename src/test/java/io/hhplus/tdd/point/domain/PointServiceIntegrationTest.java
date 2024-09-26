package io.hhplus.tdd.point.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PointServiceIntegrationTest {

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
    void 동시에_10개의_충전_요청을_보내면_정상적으로_모두_충전된다() throws ExecutionException, InterruptedException {
        // given
        int threadCount = 10;

        // when
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    pointService.charge(USER_ID, 10);
                })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // then
        UserPoint finalPoint = pointService.point(USER_ID);
        final List<PointHistory> pointHistories = pointService.history(1L);
        assertEquals(1100L,finalPoint.point());
        assertEquals(11, pointHistories.size());
    }

    @Test
    void 동시에_10개의_사용_요청을_보내면_정상적으로_모두_사용된다() throws ExecutionException, InterruptedException {
        // given
        int threadCount = 10;

        // when
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    pointService.use(USER_ID, 5);
                })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // then
        UserPoint finalPoint = pointService.point(1L);
        final List<PointHistory> pointHistories = pointService.history(1L);
        assertEquals(950L,finalPoint.point());
        assertEquals(11, pointHistories.size());
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
