package com.example.redislocktest;

public interface DepositImplement {
    void deposit(String accountId, long amount) throws InterruptedException;
}
