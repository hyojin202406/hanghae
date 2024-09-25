package io.hhplus.tdd.point.util;

import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.springframework.stereotype.Component;

@Component
public class PointPolicy {

    private static final long MAX_POINT_BALANCE = 10000; // Example maximum balance

    public void validatePointCharge(long amount) {
        if (amount < 0) {
            throw new PointValidationException("Invalid charge amount: " + amount);
        }
    }

    public void validatePointUsage(UserPoint userPoint, long amount) {
        if (amount < 0) {
            throw new PointValidationException("Invalid usage amount: " + amount);
        }

        long remainingPoint = userPoint.point() - amount;

        if (remainingPoint < 0) {
            throw new PointValidationException("Insufficient balance: attempted to use: " + amount + ", remaining balance: " + remainingPoint);
        }
    }

    public void validateMaxBalance(UserPoint userPoint) {
        if (userPoint.point() > MAX_POINT_BALANCE) {
            throw new PointValidationException("Exceeded maximum balance: " + userPoint.point());
        }
    }
}
