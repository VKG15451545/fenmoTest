package com.example.expenseTracker.expenses.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(
	@NotNull
	@DecimalMin(value = "0.00", inclusive = false, message = "amount must be positive")
	@Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
	BigDecimal amount,

	@NotBlank
	@Size(max = 64)
	String category,

	@NotBlank
	@Size(max = 512)
	String description,

	@NotNull
	LocalDate date
) {}

