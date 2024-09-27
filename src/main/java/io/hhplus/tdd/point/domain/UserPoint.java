package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.point.exception.PointValidationException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    private static final long MAX_POINT_BALANCE = 10000;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        if (amount < 0) {
            throw new PointValidationException("Invalid charge amount: " + amount);
        }

        long sumPoint = this.point() + amount;

        if (sumPoint > MAX_POINT_BALANCE) {
            throw new PointValidationException("Exceeded maximum balance: " + sumPoint);
        }
        return new UserPoint(id, sumPoint, updateMillis);
    }

    public UserPoint use(long amount) {
        if (amount < 0) {
            throw new PointValidationException("Invalid usage amount: " + amount);
        }

        long remainingPoint = this.point() - amount;

        if (remainingPoint < 0) {
            throw new PointValidationException("Insufficient balance: attempted to use: " + amount + ", remaining balance: " + remainingPoint);
        }
        return new UserPoint(id, remainingPoint, updateMillis);
    }

}