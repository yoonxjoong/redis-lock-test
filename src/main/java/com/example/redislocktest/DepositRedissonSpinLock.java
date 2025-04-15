package com.example.redislocktest;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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