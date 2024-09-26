# 동시성 제어 방식에 대한 분석 및 보고서

---

사용자 포인트 충전 및 사용 기능을 사용하는 과정에서 동시성 문제가 발생할 수 있는 상황을 고려하여, <br/>
여러 스레드가 동시에 포인트를 충전하거나 사용할 때의 동작을 테스트하였습니다.

## 동시성 문제
### 문제 설명
- 사용자 포인트를 충전하고 사용할 때, 여러 스레드가 동시에 접근하여 공유 자원(포인트)을 수정하는 상황이 발생할 수 있습니다.
  - 만약 한 스레드가 포인트를 충전하는 동안 다른 스레드가 포인트를 사용하려고 하면, 충전한 포인트가 사용된 것으로 잘못 처리될 수 있습니다.
- 이로 인해 충전과 사용의 결과가 예상과 다를 수 있으며, 데이터 무결성을 위반할 수 있습니다.

## 동시성 제어 구현
### 동시성 제어 방법 비교
동시성 제어 방법 중, synchronized, ReentrantLock을 비교한 후, ReentrantLock을 선택하게 되었습니다.
#### `synchronized`
synchronized 키워드는 메소드 전체 또는 특정 코드 블록에 적용할 수 있습니다. <br/>
메소드에 synchronized를 사용하면 해당 메소드는 한 번에 하나의 스레드만 접근할 수 있습니다.
  - 장점: 
    - synchronized 키워드가 붙은 메소드나 블록은 임계 영역(Critical Section)으로 지정되어, 동시에 여러 스레드의 접근이 제한됩니다.
  - 단점:
    - 락을 명시적으로 해제할 수 없으므로, 복잡한 시나리오에서는 제어력이 부족합니다.
#### `ReentrantLock`
  - 장점: 
    - 락에 대해 명시적으로 제어할 수 있으며, 필요할 때 락을 해제할 수 있습니다. 
    - 락을 공평하게 처리할 수 있는 fair 모드를 제공하여, 특정 스레드가 과도하게 락을 선점하는 것을 방지할 수 있습니다. 
    - 락을 여러 번 획득할 수 있는 재진입 특성을 가지고 있습니다. 
  - 단점:
    - synchronized에 비해 조금 더 복잡하며, 락 해제를 잊으면 데드락이 발생할 수 있습니다. 
    - 성능 면에서 조금 더 무거울 수 있습니다.
### ReentrantLock을 사용하게 된 이유
- 복잡한 동작에 대한 제어: charge와 use 메서드에서 포인트를 충전하고 사용하는 작업은 단순한 값 변경이 아니라, 포인트의 상태를 업데이트하는 로직을 포함하고 있습니다. 이러한 로직에 대해 안전하게 동기화를 유지하기 위해서 선택하게 되었습니다.
- 락에 대한 명시적 제어: ReentrantLock을 사용함으로써, 락을 명시적으로 제어할 수 있습니다. 충전과 사용 작업이 완료된 후에만 락을 해제하여 동시성 문제를 해결할 수 있습니다.
- 공평한 스레드 처리: ReentrantLock의 fair 옵션을 사용하여, 여러 스레드가 동시에 포인트를 처리할 때 특정 스레드가 락을 계속 점유하는 것을 방지할 수 있습니다.

### 구현 설명
#### Lock 사용 
- 각 사용자에 대한 포인트 충전 및 사용 메서드는 ReentrantLock을 사용하여 동시성을 제어합니다.
- 이를 통해 여러 스레드가 동시에 특정 사용자의 포인트를 수정하는 것을 방지합니다.
- 각 사용자 ID에 대해 별도의 락을 생성하고, 해당 락을 사용하여 포인트 충전 및 사용 작업을 안전하게 수행합니다.
#### 공통 처리 메서드
- process 메서드를 도입하여 포인트 충전과 사용의 공통 로직을 처리합니다.
- 이 메서드는 충전 또는 사용 작업에 따라 다른 동작을 수행할 수 있도록 Function 인터페이스를 활용합니다. 
- 두 메서드는 모두 process 메서드를 통해 락을 획득한 후, 포인트를 수정하고 이력을 기록합니다.
### 코드 예시
```java

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
```
## 동시성 테스트
### 테스트 설정
`PointService` 클래스의 포인트 충전 및 사용 메서드를 테스트하기 위해 <br/>
`ExecutorService`를 사용하여 여러 스레드에서 충전 및 사용 작업을 동시에 수행하도록 설정했습니다.

### 테스트 방법
- **스레드 풀 생성**: `ExecutorService`를 사용하여 여러 스레드를 생성하고, 각 스레드에서 충전 및 사용 작업을 반복하도록 설정합니다.
- **예상 값 계산**: 충전 및 사용 작업 후, 최종 포인트 잔액을 계산하고 예상 값과 비교합니다.

### 테스트 코드 예시

```java
@Test
void 동시성_제어_테스트_포인트_충전과_포인트_사용을_성공() throws Exception {

  final int threadCount = 100;
  ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
  CountDownLatch latch = new CountDownLatch(threadCount);

  IntStream.range(0, threadCount).forEach(i -> {
    executorService.execute(() -> {
      if (i % 2 == 0) {
        pointService.charge(USER_ID, 10);
        System.out.println("Charged: 10, Current Point: " + pointService.point(USER_ID).point());
      } else {
        pointService.use(USER_ID, 5);
        System.out.println("Used: 5, Current Point: " + pointService.point(USER_ID).point());
      }
      latch.countDown();
    });
  });

  latch.await();

  long expectedPoint = 1250;
  UserPoint point = pointService.point(USER_ID);

  assertEquals(expectedPoint, point.point());
}
```

## 결과 및 분석

- 동시성 테스트 결과, ReentrantLock을 사용하지 않았을 경우 예상 포인트 잔액과 실제 잔액이 다르다는 결과가 나타났습니다.
- 이는 여러 스레드가 충전 및 사용 메서드를 동시에 호출할 때, 포인트 잔액이 올바르게 업데이트되지 않음을 의미합니다.

### 실패한 경우

- 실패 테스트는 충전과 사용을 동시에 호출할 때 포인트 잔액이 예상과 다르게 나타나는 것을 확인했습니다.
- 예를 들어, 5회 충전 후 5회 사용했을 때 예상 포인트가 맞지 않았습니다.

## 개선 방안

- **동기화 처리**: 포인트 충전 및 사용 메서드에 ReentrantLock을 적용하여, 한 번에 하나의 스레드만 접근할 수 있도록 수정할 필요가 있습니다.
- **테스트 케이스 확장**: 다양한 스레드 수와 작업 빈도로 추가적인 테스트를 수행하여, 시스템의 안정성을 강화할 수 있습니다.

## 결론
동시성 문제를 해결하기 위해 적절한 동기화 메커니즘을 적용하는 것이 중요하며, 이를 통해 데이터 무결성을 보장할 수 있습니다.