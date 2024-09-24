package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;

import java.util.List;

public interface PointRepository {
    UserPoint insertOrUpdate(long id, long amount);

    void insertHistory(long id, long amount, TransactionType type, long updateMillis);

    UserPoint getUserPoint(long id);

    List<PointHistory> getUserHistory(long id);
}
