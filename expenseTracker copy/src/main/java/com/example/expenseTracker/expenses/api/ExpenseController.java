package com.example.expenseTracker.expenses.api;

import com.example.expenseTracker.expenses.api.dto.CreateExpenseRequest;
import com.example.expenseTracker.expenses.api.dto.ExpenseResponse;
import com.example.expenseTracker.expenses.service.ExpenseService;
import com.example.expenseTracker.expenses.service.IdempotencyKeyReuseException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpenseController {
	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@PostMapping("/expenses")
	public ResponseEntity<ExpenseResponse> createExpense(
		@Valid @RequestBody CreateExpenseRequest request,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
	) {
		ExpenseResponse created = expenseService.createExpense(request, idempotencyKey);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/expenses")
	public List<ExpenseResponse> listExpenses(
		@RequestParam(value = "category", required = false) String category,
		@RequestParam(value = "catagory", required = false) String catagory,
		@RequestParam(value = "sort", required = false) String sort
	) {
		// Default behavior is newest-first; "sort" accepted for forward compatibility.
		Optional<String> cat = Optional.ofNullable(category != null ? category : catagory);
		return expenseService.listExpenses(cat);
	}

	@ExceptionHandler(IdempotencyKeyReuseException.class)
	public ResponseEntity<?> handleIdempotencyKeyReuse() {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(
			new ApiError("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was reused with a different request body")
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
			.orElse("Validation error");
		return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", msg));
	}

	record ApiError(String code, String message) {}
}

