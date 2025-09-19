package com.finance.transaction.controller;

import com.finance.transaction.document.TransactionDocument;
import com.finance.transaction.dto.TransactionResponse;
import com.finance.transaction.dto.TransactionSearchResponse;
import com.finance.transaction.entity.Transaction;
import com.finance.transaction.repository.TransactionRepository;
import com.finance.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction Controller with comprehensive REST API
 * Demonstrates transaction management with Kafka integration
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for managing financial transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @Operation(
        summary = "Create new transaction",
        description = "Create a new financial transaction (income or expense)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transaction created successfully",
                content = @Content(schema = @Schema(implementation = Transaction.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @Parameter(description = "Transaction details", required = true)
            @Valid @RequestBody Transaction transaction) {
        log.info("Creating new transaction: {}", transaction);
        Transaction created = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieve a specific transaction by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found",
                content = @Content(schema = @Schema(implementation = Transaction.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId) {
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);
        return transaction.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get user transactions",
        description = "Retrieve paginated list of transactions for a specific user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            Pageable pageable) {
        
        Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @Operation(
        summary = "Update transaction",
        description = "Update an existing transaction"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction updated successfully",
                content = @Content(schema = @Schema(implementation = Transaction.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId,
            @Parameter(description = "Updated transaction details", required = true)
            @Valid @RequestBody Transaction transaction) {
        transaction.setId(transactionId);
        Transaction updated = transactionService.updateTransaction(transaction);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Delete transaction",
        description = "Delete a transaction by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId) {
        // Get the transaction first to obtain userId for cache eviction
        Optional<Transaction> existingTransaction = transactionRepository.findById(transactionId);
        if (existingTransaction.isPresent()) {
            transactionService.deleteTransaction(transactionId, existingTransaction.get().getUserId());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get cash flow analysis",
        description = "Get cash flow analytics data for transactions within a date range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/analytics/cashflow")
    public ResponseEntity<Map<String, Object>> getCashFlowAnalysis(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Start date for analytics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analytics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = transactionService.getCashFlowAnalysis(userId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }





    @Operation(
        summary = "Analyze spending patterns",
        description = "Get detailed spending pattern analysis with behavioral insights"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Spending patterns retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/analytics/spending-patterns")
    public ResponseEntity<Map<String, Object>> analyzeSpendingPatterns(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Minimum frequency threshold")
            @RequestParam(defaultValue = "3") Integer minFrequency) {
        
        Map<String, Object> patterns = transactionService.analyzeSpendingPatterns(userId, startDate, minFrequency);
        return ResponseEntity.ok(patterns);
    }




}