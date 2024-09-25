package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.exception.InvalidUserIdException;
import io.hhplus.tdd.point.exception.UserPointNotFoundException;
import io.hhplus.tdd.point.repository.PointRepository;
import io.hhplus.tdd.point.util.PointPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final PointRepository pointRepository;
    private final PointPolicy pointPolicy;

    public PointService(PointRepository pointRepository, PointPolicy pointPolicy) {
        this.pointRepository = pointRepository;
        this.pointPolicy = pointPolicy;
    }

    /**
     * 유저 포인트 조회
     * @param id
     * @returna
     */
    public UserPoint getUserPoint(long id) {
        // 유효하지 않은 ID 검사
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        return pointRepository.getUserPoint(id);
    }

    /**
     * 유저 포인트 충전/이용 내역 조회
     * @param id
     * @return
     */
    public List<PointHistory> getUserHistory(long id) {
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
    public UserPoint chargeUserPoint(long id, long amount) {
        // 유효하지 않은 ID 검사
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        // 포인트 충전 유효성 검사
        pointPolicy.validatePointCharge(amount);

        // 유저 포인트 충전
        UserPoint updatedUserPoint = pointRepository.insertOrUpdate(id, amount);

        // 최대 잔고 검사
        pointPolicy.validateMaxBalance(updatedUserPoint);

        // 유저 포인트 충전 내역 저장
        pointRepository.insertHistory(id,
                amount,
                TransactionType.CHARGE,
                updatedUserPoint.updateMillis());

        return updatedUserPoint;
    }

    /**
     * 유저 포인트 사용 기능
     * @param id
     * @param amount
     * @return
     */
    public UserPoint useUserPoint(long id, long amount) {
        // 유효하지 않은 ID 검사
        if (id <= 0) {
            throw new InvalidUserIdException("Invalid user ID: " + id);
        }

        // 유저 포인트 조회
        UserPoint currentUserPoint = pointRepository.getUserPoint(id);

        // 유저 포인트 조회 실패 시 예외 처리
        if (currentUserPoint == null) {
            throw new UserPointNotFoundException("Failed to find user point for ID: " + id);
        }

        // 포인트 사용 유효성 검사
        pointPolicy.validatePointUsage(currentUserPoint, amount);

        // 포인트 업데이트 및 변환
        UserPoint userUpdatedPoint = pointRepository.insertOrUpdate(id, currentUserPoint.point() - amount);

        // 포인트 사용 내역 저장
        pointRepository.insertHistory(
                id,
                amount,
                TransactionType.USE,
                userUpdatedPoint.updateMillis()
        );

        return userUpdatedPoint;
    }

}