package io.hhplus.tdd.point.infra;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.PointRepository;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointRepositoryImpl implements PointRepository {

    PointHistoryTable pointHistoryTable = new PointHistoryTable();
    UserPointTable userPointTable = new UserPointTable();

    @Override
    public UserPoint insertOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

    @Override
    public PointHistory insertHistory(long id, long amount, TransactionType type, long updateMillis) {
        return pointHistoryTable.insert(id, amount, type, updateMillis);
    }

    @Override
    public UserPoint point(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public List<PointHistory> getUserHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

}