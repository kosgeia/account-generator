package com.example.account.accountgenerator.repository;


import com.example.account.accountgenerator.entity.AccountEntity;
import com.example.account.accountgenerator.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findFirstByStatus(AccountStatus status); // Fetch first UNUSED account

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE account_entity a
        SET a.status = 'PENDING'
        WHERE a.account_number = (
            SELECT account_number FROM account_entity 
            WHERE status = 'UNUSED'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
        )
        RETURNING a.account_number
    """, nativeQuery = true)
    String fetchAndMarkPending();

    @Modifying
    @Transactional
    @Query("UPDATE AccountEntity a SET a.status = 'ASSIGNED' WHERE a.accountNumber = :accountNumber")
    void markAsAssigned(@Param("accountNumber") String accountNumber);

    @Modifying
    @Transactional
    @Query("UPDATE AccountEntity a SET a.status = 'UNUSED' WHERE a.accountNumber = :accountNumber")
    void restoreAccount(@Param("accountNumber") String accountNumber);

    @Modifying
    @Transactional
    @Query("UPDATE AccountEntity a SET a.status = :status WHERE a.accountNumber = :accountNumber")
    void updateAccountStatus(@Param("accountNumber") String accountNumber, @Param("status") AccountStatus status);
}
