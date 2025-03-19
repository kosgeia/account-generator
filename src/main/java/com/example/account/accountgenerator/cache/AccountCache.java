package com.example.account.accountgenerator.cache;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
public class AccountCache {
    private final Queue<String> cacheQueue = new LinkedList<>();

    public synchronized void addToCache(String accountNumber) {
        cacheQueue.offer(accountNumber);
    }

    public synchronized String getFromCache() {
        return cacheQueue.poll();
    }

    public synchronized boolean isCacheEmpty() {
        return cacheQueue.isEmpty();
    }
}
