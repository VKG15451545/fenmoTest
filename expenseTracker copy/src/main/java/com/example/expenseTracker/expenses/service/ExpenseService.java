package com.example.expenseTracker.expenses.service;

import com.example.expenseTracker.expenses.api.dto.CreateExpenseRequest;
import com.example.expenseTracker.expenses.api.dto.ExpenseResponse;
import com.example.expenseTracker.expenses.model.Expense;
import com.example.expenseTracker.expenses.repo.ExpenseRepository;
import com.example.expenseTracker.idempotency.model.IdempotencyRecord;
import com.example.expenseTracker.idempotency.repo.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
	private final ExpenseRepository expenseRepository;
	private final IdempotencyRecordRepository idempotencyRecordRepository;
	private final ObjectMapper objectMapper;

	public ExpenseService(
		ExpenseRepository expenseRepository,
		IdempotencyRecordRepository idempotencyRecordRepository,
		ObjectMapper objectMapper
	) {
		this.expenseRepository = expenseRepository;
		this.idempotencyRecordRepository = idempotencyRecordRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public ExpenseResponse createExpense(CreateExpenseRequest request, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			Expense created = persistExpense(request);
			return ExpenseMapper.toResponse(created);
		}

		String requestHash = sha256Hex(stableIdempotencyPayload(request));
		Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findById(idempotencyKey);
		if (existing.isPresent()) {
			IdempotencyRecord rec = existing.get();
			if (!rec.getRequestHash().equals(requestHash)) {
				throw new IdempotencyKeyReuseException();
			}
			return readResponseFromRecord(rec);
		}

		Expense created = persistExpense(request);
		ExpenseResponse response = ExpenseMapper.toResponse(created);
		String body = writeJson(response);

		IdempotencyRecord rec = new IdempotencyRecord();
		rec.setKey(idempotencyKey);
		rec.setRequestHash(requestHash);
		rec.setStatusCode(201);
		rec.setResponseBody(body);

		try {
			idempotencyRecordRepository.save(rec);
		} catch (DataIntegrityViolationException e) {
			// Another request won the race; return the stored response.
			IdempotencyRecord winner = idempotencyRecordRepository.findById(idempotencyKey).orElseThrow();
			if (!winner.getRequestHash().equals(requestHash)) {
				throw new IdempotencyKeyReuseException();
			}
			return readResponseFromRecord(winner);
		}

		return response;
	}

	@Transactional(readOnly = true)
	public List<ExpenseResponse> listExpenses(Optional<String> categoryOpt) {
		List<Expense> expenses = categoryOpt
			.filter(s -> !s.isBlank())
			.map(String::trim)
			.map(expenseRepository::findByCategoryIgnoreCase)
			.orElseGet(expenseRepository::findAll);

		return expenses.stream()
			.sorted(Comparator.comparing(Expense::getDate).reversed().thenComparing(Expense::getCreatedAt).reversed())
			.map(ExpenseMapper::toResponse)
			.toList();
	}

	private Expense persistExpense(CreateExpenseRequest request) {
		Expense e = new Expense();
		e.setAmount(request.amount().setScale(2));
		e.setCategory(request.category().trim());
		e.setDescription(request.description().trim());
		LocalDate date = request.date();
		e.setDate(date);
		return expenseRepository.save(e);
	}

	private ExpenseResponse readResponseFromRecord(IdempotencyRecord rec) {
		try {
			return objectMapper.readValue(rec.getResponseBody(), ExpenseResponse.class);
		} catch (Exception e) {
			throw new IllegalStateException("Stored idempotency response is unreadable", e);
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize response", e);
		}
	}

	private static String stableIdempotencyPayload(CreateExpenseRequest request) {
		// Keep it stable across retries; whitespace trimmed by validation/service.
		return String.join("|",
			request.amount().setScale(2).toPlainString(),
			request.category().trim().toLowerCase(),
			request.description().trim(),
			request.date().toString()
		);
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}

