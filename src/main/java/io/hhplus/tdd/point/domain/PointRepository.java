package io.hhplus.tdd.point.domain;

import java.util.List;

public interface PointRepository {
    UserPoint insertOrUpdate(long id, long amount);

    PointHistory insertHistory(long id, long amount, TransactionType type, long updateMillis);

    UserPoint point(long id);

    List<PointHistory> getUserHistory(long id);
}
