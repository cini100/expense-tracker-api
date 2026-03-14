package com.example.expensetracker.transaction.repository;

import com.example.expensetracker.transaction.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long>,
        JpaSpecificationExecutor<TransactionRecord> {

    Optional<TransactionRecord> findByIdAndUserId(Long id, Long userId);
}
