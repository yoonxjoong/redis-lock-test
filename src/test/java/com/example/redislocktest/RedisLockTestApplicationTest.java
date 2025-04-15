package com.example.redislocktest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
public class RedisLockTestApplicationTest {

    @Autowired
    private DepositRedissonLock pubSubLockService;

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



