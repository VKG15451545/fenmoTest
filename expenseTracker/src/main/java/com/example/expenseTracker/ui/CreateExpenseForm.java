package com.example.expenseTracker.ui;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateExpenseForm {
	@NotNull
	@DecimalMin(value = "0.00", inclusive = false, message = "amount must be positive")
	@Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
	private BigDecimal amount;

	@NotBlank
	@Size(max = 64)
	private String category;

	@NotBlank
	@Size(max = 512)
	private String description;

	@NotNull
	private LocalDate date;

	@NotBlank
	@Size(max = 128)
	private String idempotencyKey;

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}
}

