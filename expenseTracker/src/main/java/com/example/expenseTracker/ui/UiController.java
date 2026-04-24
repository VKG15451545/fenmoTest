package com.example.expenseTracker.ui;

import com.example.expenseTracker.expenses.api.dto.CreateExpenseRequest;
import com.example.expenseTracker.expenses.api.dto.ExpenseResponse;
import com.example.expenseTracker.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UiController {
	private final ExpenseService expenseService;

	public UiController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@GetMapping("/")
	public String index(
		@RequestParam(value = "category", required = false) String category,
		Model model
	) {
		CreateExpenseForm form = new CreateExpenseForm();
		form.setIdempotencyKey(UUID.randomUUID().toString());
		model.addAttribute("form", form);

		Optional<String> categoryOpt = Optional.ofNullable(category).filter(s -> !s.isBlank());
		List<ExpenseResponse> expenses = expenseService.listExpenses(categoryOpt);

		BigDecimal total = expenses.stream()
			.map(er -> new BigDecimal(er.amount()))
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);

		model.addAttribute("selectedCategory", categoryOpt.orElse(""));
		model.addAttribute("expenses", expenses);
		model.addAttribute("total", total.toPlainString());
		model.addAttribute("categories", expenses.stream().map(ExpenseResponse::category).distinct().sorted().toList());
		return "index";
	}

	@PostMapping("/ui/expenses")
	public String createExpense(
		@Valid @ModelAttribute("form") CreateExpenseForm form,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes,
		Model model
	) {
		if (bindingResult.hasErrors()) {
			// Re-render page with existing list/total so the user doesn’t lose context.
			List<ExpenseResponse> expenses = expenseService.listExpenses(Optional.empty());
			BigDecimal total = expenses.stream()
				.map(er -> new BigDecimal(er.amount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
			model.addAttribute("selectedCategory", "");
			model.addAttribute("expenses", expenses);
			model.addAttribute("total", total.toPlainString());
			model.addAttribute("categories", expenses.stream().map(ExpenseResponse::category).distinct().sorted().toList());
			return "index";
		}

		expenseService.createExpense(
			new CreateExpenseRequest(form.getAmount(), form.getCategory(), form.getDescription(), form.getDate()),
			form.getIdempotencyKey()
		);

		// PRG avoids duplicate submissions on refresh.
		redirectAttributes.addAttribute("category", "");
		return "redirect:/";
	}
}

