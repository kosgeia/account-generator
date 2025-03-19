package com.example.account.accountgenerator.service;


import com.example.account.accountgenerator.cache.AccountCache;
import com.example.account.accountgenerator.entity.AccountEntity;
import com.example.account.accountgenerator.entity.AccountStatus;
import com.example.account.accountgenerator.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountGeneratorService {
    private final AccountRepository accountRepository;
    private final AccountCache accountCache;

    @Value("${account.cache.batch-size}")
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
        account.ifPresentOrElse(
                acc -> accountCache.addToCache(acc.getAccountNumber()),
                this::generateNewAccounts // Generate new accounts if no UNUSED found
        );

        // Prefetch additional accounts as needed
        while (accountCache.isCacheEmpty() || accountCache.getFromCache() == null) {
            generateNewAccounts();
        }
    }

    private void generateNewAccounts() {
        for (int i = 0; i < batchSize; i++) {
            String accountNumber = "2200" + (System.currentTimeMillis() % 99999999); // Simplified for demo
            AccountEntity account = new AccountEntity(); // Status = UNUSED
            account.setAccountNumber(accountNumber);
            account.setStatus(AccountStatus.UNUSED);
            accountRepository.save(account);
            accountCache.addToCache(accountNumber);
        }
    }
}
