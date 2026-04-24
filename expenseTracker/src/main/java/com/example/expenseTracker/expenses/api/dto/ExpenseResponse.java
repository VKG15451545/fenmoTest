package com.example.expenseTracker.expenses.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
	Long id,
	String amount,
	String category,
	String description,
	LocalDate date,
	Instant createdAt
) {}

