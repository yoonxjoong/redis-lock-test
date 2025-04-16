# Redisson Lock 테스트

## Redis pubsub Lock vs Redis SpinLock
- 동시성 락 Redisson 소스를 분석하면서 redisson pubsub 락과 spin락과의 성능 차이가 궁금해서 테스트를 해보았습니다.


## 소스 설명

- RedisLockTestConfig.java 
```java
/*
 * 따로 서버 정보는 설정하지 않았습니다. 기본으로 localhost:6379로 설정되어 있음
 * */
@Configuration
public class RedisLockTestConfig {

    @Bean
    public RedissonClient redissonClient() {
        return Redisson.create();
    }
}
```

- DepositImplement.java
```java
/**
 *  입금 처리를 위한 공통 인터페이스
 */
public interface DepositImplement {
    void deposit(String accountId, long amount) throws InterruptedException;

}
```

- DepositRedissonLock.java
```java
/**
 * redissonClient의 getLock(기본으로 pub/sub 기반 Lock를 사용하는 함수)를 사용한 클래스
 */

@Component
public class DepositRedissonLock implements DepositImplement{

    private final Logger logger = LoggerFactory.getLogger(DepositRedissonLock.class);

    private final RedissonClient redissonClient;

    public DepositRedissonLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void deposit(String accountId, long amount) throws InterruptedException {
        RLock rLock = redissonClient.getLock("lock:account:" + accountId);

        if (rLock.tryLock(100, 1000, TimeUnit.MILLISECONDS)) {
            try {
                Thread.sleep(1000);
            } finally {
                rLock.unlock();
            }
        }
    }
}

```

- DepositRedissonSpinLock.java
```java
/**
 * 스핀락을 사용한 클래스 (while문을 돌면서 락을 획득한다)
 */
@Component
public class DepositRedissonSpinLock implements DepositImplement {

    private final Logger logger = LoggerFactory.getLogger(DepositRedissonSpinLock.class);
    private final RedissonClient redissonClient;

    public DepositRedissonSpinLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void deposit(String accountId, long amount) throws InterruptedException {
        RLock rLock = redissonClient.getLock("lock:account:" + accountId);

        boolean acquired = false;
        while (!acquired) {
            acquired = rLock.tryLock(100, 1000, TimeUnit.MILLISECONDS);
        }

        try {
            Thread.sleep(1000);
        } finally {
            rLock.unlock();
        }
    }
}
```

## 테스트 소스
- RedisLockTestApplicationTest.java
```java

@SpringBootTest
public class RedisLockTestApplicationTest {

    // pubsub 기반 레디스 락 생성
    @Autowired
    private DepositRedissonLock pubSubLockService;

    // 스핀락 기반 레디스 락 생성
    @Autowired
    private DepositRedissonSpinLock spinLockService;

    private final int THREAD_COUNT = 50;
    private final long DEPOSIT_AMOUNT = 1000L;


    private void runDepositTest(String name, DepositImplement service) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        StopWatch stopWatch = new StopWatch();

        stopWatch.start(name);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    service.deposit("account-x", DEPOSIT_AMOUNT);
                } catch (Exception e) {
                    // 
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        stopWatch.stop();

        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    void spinLock_그리고_pubSubLock_성능비교() throws InterruptedException {
        System.out.println("=== SpinLock 테스트 시작 ===");
        runDepositTest("spinLock", spinLockService);

        System.out.println("=== PubSubLock 테스트 시작 ===");
        runDepositTest("pubSubLock", pubSubLockService);
    }
}

```

- 테스트 결과
```plain
=== SpinLock 테스트 시작 ===
StopWatch '': 50.119077083 seconds
----------------------------------------
Seconds       %       Task name
----------------------------------------
50.11907708   100%    spinLock

=== PubSubLock 테스트 시작 ===
StopWatch '': 1.044971375 seconds
----------------------------------------
Seconds       %       Task name
----------------------------------------
1.044971375   100%    pubSubLock
```


## 의의
이 테스트는 Redis를 사용한 두 가지 다른 락 메커니즘인 Pub/Sub Lock과 Spin Lock의 성능 차이를 비교하는 데 중점을 두었습니다.
테스트 결과는 두 락 메커니즘의 동작 방식과 성능 특성을 명확히 보여주며, 다음과 같은 중요한 의의를 가집니다.

1. 성능 차이의 명확한 증명
   테스트 결과에서 Spin Lock이 약 50초가 소요된 반면, Pub/Sub Lock은 약 1초에 불과했습니다. 
    Spin Lock은 락을 획득하기 위해 지속적으로 시도하는 방식으로, CPU 자원을 소모하며 대기하는 동안 다른 작업을 수행할 수 없게 됩니다. 반면, Pub/Sub Lock은 Redis의 Pub/Sub 메커니즘을 활용하여 락을 관리하므로, 더 적은 자원으로도 높은 성능을 발휘할 수 있습니다.

2. 동시성 처리의 중요성
   이 테스트는 동시성 처리에서 락의 선택이 성능에 미치는 영향을 강조합니다. 다수의 스레드가 동시에 입금 작업을 수행할 때, 락의 구현 방식에 따라 전체 시스템의 성능이 크게 달라질 수 있습니다. Pub/Sub Lock은 Redis의 분산 특성을 활용하여 여러 클라이언트 간의 동시성을 효과적으로 관리할 수 있습니다.


## redis-lock-test 실행 방법

### 1. Docker 설치되어 있어야 합니다.

### 2. 실행 명령
```bash
docker-compose up
```