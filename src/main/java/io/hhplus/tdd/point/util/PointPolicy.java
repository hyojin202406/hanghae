package io.hhplus.tdd.point.util;

import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.springframework.stereotype.Component;

@Component
public class PointPolicy {

    private static final long MAX_POINT_BALANCE = 10000; // 최대 잔고 예시

    public void validatePointCharge(long amount) {
        if (amount < 0) {
            throw new PointValidationException("충전할 수 없는 금액입니다: " + amount);
        }
    }

    public void validatePointUsage(UserPoint userPoint, long amount) {
        if (amount < 0) {
            throw new PointValidationException("사용할 수 없는 금액입니다: " + amount);
        }

        long remainingPoint = userPoint.point() - amount;

        if (remainingPoint < 0) {
            throw new PointValidationException("잔고 부족: 사용하려는 포인트: " + amount + ", 잔여 포인트: " + remainingPoint);
        }
    }

    public void validateMaxBalance(UserPoint userPoint) {
        if (userPoint.point() > MAX_POINT_BALANCE) {
            throw new PointValidationException("최대 잔고를 초과하였습니다: " + userPoint.point());
        }
    }

}