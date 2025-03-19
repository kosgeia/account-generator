package com.example.account.accountgenerator.service;


import com.example.account.accountgenerator.cache.AccountCache;
import com.example.account.accountgenerator.entity.AccountEntity;
import com.example.account.accountgenerator.entity.AccountStatus;
import com.example.account.accountgenerator.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AccountGeneratorService {
    private static final long EPOCH = 1700000000000L; // Custom epoch
    private static final AtomicInteger sequence = new AtomicInteger(0);
    private static final int MAX_SEQUENCE = 99; // 2-digit sequence ensures a total of 8 digits

    private final AccountRepository accountRepository;
    private final AccountCache accountCache;

    @Value("${account.cache.batch-size:10}")
    private int batchSize;

    public AccountGeneratorService(AccountRepository accountRepository, AccountCache accountCache) {
        this.accountRepository = accountRepository;
        this.accountCache = accountCache;
    }

    public synchronized String getNextAccount() {
        if (accountCache.isCacheEmpty()) {
            prefetchAccounts();
        }
        String accountNumber = accountCache.getFromCache();
        if (accountNumber != null) {
            accountRepository.markAsAssigned(accountNumber); // Mark as ASSIGNED
        }
        return accountNumber;
    }

    public synchronized void returnAccount(String accountNumber) {
        accountCache.addToCache(accountNumber);
        accountRepository.restoreAccount(accountNumber); // Restore as UNUSED
    }

    private void prefetchAccounts() {
        // Fetch unused accounts first
        Optional<AccountEntity> account = accountRepository.findFirstByStatus(AccountStatus.UNUSED);
        account.ifPresentOrElse(accountEntity -> accountCache.addToCache(accountEntity.getAccountNumber()),
                () -> {
                    // Generate new accounts if no UNUSED found
                    log.warn("Generating new accounts, cache is depleted");
                    generateNewAccounts();
                }
        );
    }

    private void generateNewAccounts() {
        for (int i = 0; i < batchSize; i++) {
            String accountNumber = generateAccountNumber();
            AccountEntity account = new AccountEntity(); // Status = UNUSED
            account.setAccountNumber(accountNumber);
            account.setStatus(AccountStatus.UNUSED);
            try {
                accountRepository.save(account);
                accountCache.addToCache(accountNumber);
            } catch (RuntimeException exception) {
                log.error("Attempting to generate and cache an existing account number, dropping account {} ", accountNumber);
            }
        }
    }

    public static synchronized String generateAccountNumber() {
        long timestamp = (System.currentTimeMillis() - EPOCH) % 1_000_000; // 6-digit timestamp
        int seq = sequence.updateAndGet(n -> (n >= MAX_SEQUENCE) ? 0 : n + 1); // 2-digit sequence
        return String.format("2200%06d%02d", timestamp, seq);
    }
}