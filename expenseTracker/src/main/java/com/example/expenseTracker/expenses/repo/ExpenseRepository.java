package com.example.expenseTracker.expenses.repo;

import com.example.expenseTracker.expenses.model.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
	List<Expense> findByCategoryIgnoreCase(String category);
}

