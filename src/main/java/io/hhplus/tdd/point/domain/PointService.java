package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.point.exception.InvalidUserIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final PointRepository pointRepository;

    private final ReentrantLock lock = new ReentrantLock();

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

    /**
     * 유저 포인트 충전 기능
     * @param id
     * @param amount
     * @return
     */
    public UserPoint charge(long id, long amount) {
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }
        final UserPoint point = pointRepository.point(id);
        final UserPoint chargedPoint = point.charge(amount);
        final UserPoint updatedUserPoint = pointRepository.insertOrUpdate(id, chargedPoint.point());
        pointRepository.insertHistory(id, amount, TransactionType.CHARGE, updatedUserPoint.updateMillis());
        return updatedUserPoint;
    }

    /**
     * 유저 포인트 사용 기능
     * @param id
     * @param amount
     * @return
     */
    public UserPoint use(long id, long amount) {

        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }
        final UserPoint point = pointRepository.point(id);
        final UserPoint usedPoint = point.use(amount);
        UserPoint updatedPoint = pointRepository.insertOrUpdate(id, usedPoint.point());
        pointRepository.insertHistory(id, amount, TransactionType.USE, updatedPoint.updateMillis());
        return updatedPoint;

    }

}