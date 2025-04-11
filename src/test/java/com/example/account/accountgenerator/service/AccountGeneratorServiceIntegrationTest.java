package com.example.account.accountgenerator.service;

import com.example.account.accountgenerator.cache.AccountCache;
import com.example.account.accountgenerator.entity.AccountEntity;
import com.example.account.accountgenerator.entity.AccountStatus;
import com.example.account.accountgenerator.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountGeneratorServiceIntegrationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountCache accountCache;

    @InjectMocks
    private AccountGeneratorService accountGeneratorService;

    @BeforeEach
    void setUp() {
        // Ensure batch size is known for deterministic testing
        ReflectionTestUtils.setField(accountGeneratorService, "batchSize", 2);
        // Reset sequence for consistent account generation
        ReflectionTestUtils.setField(AccountGeneratorService.class, "sequence", new AtomicInteger(0));
    }

    @Test
    void testGetNextAccount_whenCacheIsNotEmpty_returnsFromCacheAndMarksAssigned() {
        String mockAccountNumber = "220012345600";
        when(accountCache.isCacheEmpty()).thenReturn(false);
        when(accountCache.getFromCache()).thenReturn(mockAccountNumber);

        String result = accountGeneratorService.getNextAccount();

        assertEquals(mockAccountNumber, result);
        verify(accountRepository).markAsAssigned(mockAccountNumber);
    }

    @Test
    void testReturnAccount_addsToCacheAndRestoresAccount() {
        String accountNumber = "220012345600";

        accountGeneratorService.returnAccount(accountNumber);

        verify(accountCache).addToCache(accountNumber);
        verify(accountRepository).restoreAccount(accountNumber);
    }

    @Test
    void testGetNextAccount_whenCacheIsEmpty_fetchesUnusedOrGeneratesNew() {
        when(accountCache.isCacheEmpty()).thenReturn(true);
        when(accountRepository.findFirstByStatus(AccountStatus.UNUSED)).thenReturn(Optional.empty());
        doNothing().when(accountCache).addToCache(anyString());

        accountGeneratorService.getNextAccount();

        verify(accountRepository, atLeastOnce()).save(any(AccountEntity.class));
        verify(accountCache, atLeastOnce()).addToCache(anyString());
    }

    @Test
    void testGenerateAccountNumber_formatAndUniqueness() {
        String first = AccountGeneratorService.generateAccountNumber();
        String second = AccountGeneratorService.generateAccountNumber();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
        assertTrue(first.startsWith("2200"));
        assertEquals(12, first.length());
    }
}
