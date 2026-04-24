package com.example.expenseTracker.idempotency.repo;

import com.example.expenseTracker.idempotency.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {}

