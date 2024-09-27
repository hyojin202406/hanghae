package io.hhplus.tdd.point.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    PointService pointService;

    private static final long USER_ID = 1L;

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
        assertEquals(100,finalPoint.point());
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
        assertEquals(50,finalPoint.point());
    }

}
