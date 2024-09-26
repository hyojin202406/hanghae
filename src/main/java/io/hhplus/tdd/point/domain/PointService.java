package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.point.exception.InvalidUserIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Service
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final PointRepository pointRepository;

    private final Map<Long, Lock> locks = new ConcurrentHashMap<>();

    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    /**
     * 유저 포인트 조회
     * @param id
     * @returna
     */
    public UserPoint point(long id) {
        // 유효하지 않은 ID 검사
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        return pointRepository.point(id);
    }

    /**
     * 유저 포인트 충전/이용 내역 조회
     * @param id
     * @return
     */
    public List<PointHistory> history(long id) {
        // 유효하지 않은 ID 검사
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        return pointRepository.getUserHistory(id);
    }

    public UserPoint charge(long id, long amount) {
        return process(id, amount, (point) -> {
            final UserPoint chargedPoint = point.charge(amount);
            pointRepository.insertHistory(id, amount, TransactionType.CHARGE, chargedPoint.updateMillis());
            return chargedPoint;
        });
    }

    public UserPoint use(long id, long amount) {
        return process(id, amount, (point) -> {
            final UserPoint usedPoint = point.use(amount);
            if (usedPoint.point() < 0) {
                throw new IllegalStateException("Insufficient points");
            }
            pointRepository.insertHistory(id, amount, TransactionType.USE, usedPoint.updateMillis());
            return usedPoint;
        });
    }

    private UserPoint process(long id, long amount, Function<UserPoint, UserPoint> operation) {
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        final Lock lock = locks.computeIfAbsent(id, userId -> new ReentrantLock(true));
        lock.lock();

        try {
            final UserPoint point = pointRepository.point(id);
            if (point == null) {
                throw new IllegalStateException("User point not found");
            }

            UserPoint updatedPoint = operation.apply(point);
            return pointRepository.insertOrUpdate(id, updatedPoint.point());
        } finally {
            lock.unlock();
        }
    }

}