package com.example.expenseTracker.expenses.service;

import com.example.expenseTracker.expenses.api.dto.ExpenseResponse;
import com.example.expenseTracker.expenses.model.Expense;
import java.math.RoundingMode;

final class ExpenseMapper {
	private ExpenseMapper() {}

	static ExpenseResponse toResponse(Expense e) {
		return new ExpenseResponse(
			e.getId(),
			e.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
			e.getCategory(),
			e.getDescription(),
			e.getDate(),
			e.getCreatedAt()
		);
	}
}

