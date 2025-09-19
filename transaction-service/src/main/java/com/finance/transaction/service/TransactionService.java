package com.finance.transaction.service;

import com.finance.transaction.document.TransactionDocument;
import com.finance.transaction.dto.TransactionResponse;
import com.finance.transaction.dto.TransactionSearchResponse;
import com.finance.transaction.entity.Transaction;
import com.finance.transaction.repository.TransactionRepository;
import com.finance.transaction.repository.TransactionSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transaction Service with comprehensive financial operations
 * Demonstrates Spring IoC, Java Streams, Caching, and Kafka integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionSearchRepository transactionSearchRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    private static final String TRANSACTION_ANALYTICS_TOPIC = "transaction-analytics";

    /**
     * Create new transaction with event publishing and search indexing
     * Demonstrates comprehensive transaction processing
     */
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating transaction for user: {}, amount: {}", 
                transaction.getUserId(), transaction.getAmount());
        
        // Validate transaction using Java Streams
        validateTransaction(transaction);
        
        // Save to PostgreSQL
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Index in Elasticsearch for search
        TransactionDocument document = mapToDocument(savedTransaction);
        transactionSearchRepository.save(document);
        
        // Publish to Kafka for real-time processing
        publishTransactionEvent(savedTransaction);
        
        // Clear relevant caches
        evictUserCaches(transaction.getUserId());
        
        log.info("Transaction created successfully: {}", savedTransaction.getId());
        return savedTransaction;
    }

    /**
     * Get transactions with caching strategy
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(UUID userId, Pageable pageable) {
        log.debug("Fetching transactions for user: {}", userId);
        Page<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId, pageable);
        return transactions.map(this::mapToResponse);
    }

    /**
     * Search transactions using Elasticsearch with advanced queries
     * Demonstrates Elasticsearch integration and Java Streams processing
     */


    /**
     * Get monthly category spending with advanced analytics
     * Demonstrates complex data processing with Java Streams
     */


    /**
     * Get cash flow analysis with comprehensive insights
     * Demonstrates advanced financial analytics with Java Streams
     */
    @Cacheable(value = "cashflow-analysis", key = "#userId + '_' + #startDate + '_' + #endDate")
    @Transactional(readOnly = true)
    public Map<String, Object> getCashFlowAnalysis(UUID userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Performing cash flow analysis for user: {}", userId);
        
        List<Object[]> rawData = transactionRepository.getCashFlowAnalysis(userId, startDate, endDate);
        
        List<Map<String, Object>> dailyCashFlow = rawData.stream()
            .map(this::mapCashFlowData)
            .collect(Collectors.toList());
        
        // Calculate summary statistics using Java Streams
        Map<String, Object> summary = dailyCashFlow.stream()
            .collect(Collectors.teeing(
                Collectors.summingDouble(day -> ((BigDecimal) day.get("dailyIncome")).doubleValue()),
                Collectors.summingDouble(day -> ((BigDecimal) day.get("dailyExpenses")).doubleValue()),
                (totalIncome, totalExpenses) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("totalIncome", BigDecimal.valueOf(totalIncome));
                    result.put("totalExpenses", BigDecimal.valueOf(totalExpenses));
                    result.put("netCashFlow", BigDecimal.valueOf(totalIncome - totalExpenses));
                    result.put("avgDailyIncome", BigDecimal.valueOf(totalIncome / dailyCashFlow.size()));
                    result.put("avgDailyExpenses", BigDecimal.valueOf(totalExpenses / dailyCashFlow.size()));
                    return result;
                }
            ));
        
        Map<String, Object> period = new HashMap<>();
        period.put("startDate", startDate);
        period.put("endDate", endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("dailyData", dailyCashFlow);
        result.put("period", period);
        return result;
    }

    /**
     * Analyze spending patterns with behavioral insights
     * Demonstrates complex analytical processing with Java Streams
     */
    @Cacheable(value = "spending-patterns", key = "#userId + '_' + #startDate + '_' + #minFrequency")
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeSpendingPatterns(UUID userId, LocalDate startDate, Integer minFrequency) {
        log.debug("Analyzing spending patterns for user: {}", userId);
        
        List<Object[]> rawData = transactionRepository.getSpendingPatterns(userId, startDate, minFrequency);
        
        // Process and group data using Java Streams
        Map<String, List<Map<String, Object>>> patternsByDay = rawData.stream()
            .map(this::mapSpendingPattern)
            .collect(Collectors.groupingBy(pattern -> (String) pattern.get("dayName")));
        
        Map<String, List<Map<String, Object>>> patternsByTime = rawData.stream()
            .map(this::mapSpendingPattern)
            .collect(Collectors.groupingBy(pattern -> (String) pattern.get("timePeriod")));
        
        // Calculate insights using advanced stream operations
        Map<String, Object> insights = rawData.stream()
            .map(this::mapSpendingPattern)
            .collect(Collectors.teeing(
                Collectors.groupingBy(
                    pattern -> (String) pattern.get("categoryName"),
                    Collectors.summingDouble(pattern -> ((BigDecimal) pattern.get("totalAmount")).doubleValue())
                ),
                Collectors.groupingBy(
                    pattern -> (String) pattern.get("dayName"),
                    Collectors.counting()
                ),
                (categoryTotals, dayFrequencies) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("topCategories", getTopCategories(categoryTotals, 5));
                    result.put("mostActiveDays", dayFrequencies);
                    result.put("totalPatterns", rawData.size());
                    return result;
                }
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("patternsByDay", patternsByDay);
        result.put("patternsByTime", patternsByTime);
        result.put("insights", insights);
        result.put("analysisDate", LocalDateTime.now());
        return result;
    }

    /**
     * Get budget variance analysis with alerts
     */


    /**
     * Detect anomalous transactions using statistical analysis
     * Demonstrates advanced analytics and fraud detection
     */


    /**
     * Update transaction with cache eviction and re-indexing
     */
    @CacheEvict(value = {"user-transactions", "monthly-spending", "cashflow-analysis", 
                        "spending-patterns", "budget-variance"}, key = "#transaction.userId")
    public Transaction updateTransaction(Transaction transaction) {
        log.info("Updating transaction: {}", transaction.getId());
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        // Update Elasticsearch index
        TransactionDocument document = mapToDocument(updatedTransaction);
        transactionSearchRepository.save(document);
        
        // Publish update event
        publishTransactionUpdateEvent(updatedTransaction);
        
        return updatedTransaction;
    }

    /**
     * Delete transaction with cleanup
     */
    @CacheEvict(value = {"user-transactions", "monthly-spending", "cashflow-analysis", 
                        "spending-patterns", "budget-variance"}, key = "#userId")
    public void deleteTransaction(UUID transactionId, UUID userId) {
        log.info("Deleting transaction: {}", transactionId);
        
        transactionRepository.deleteById(transactionId);
        transactionSearchRepository.deleteById(transactionId.toString());
        
        // Publish deletion event
        publishTransactionDeletionEvent(transactionId, userId);
    }

    // Private helper methods

    private void validateTransaction(Transaction transaction) {
        List<String> errors = new ArrayList<>();
        
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be positive");
        }
        
        if (transaction.getUserId() == null) {
            errors.add("User ID is required");
        }
        
        if (transaction.getAccountId() == null) {
            errors.add("Account ID is required");
        }
        
        if (transaction.getTransactionDate() == null) {
            errors.add("Transaction date is required");
        }
        
        if (!errors.isEmpty()) {
            throw new RuntimeException("Validation failed: " + String.join(", ", errors));
        }
    }

    private TransactionDocument mapToDocument(Transaction transaction) {
        return TransactionDocument.builder()
            .id(transaction.getId().toString())
            .userId(transaction.getUserId())
            .accountId(transaction.getAccountId())
            .categoryId(transaction.getCategoryId())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .description(transaction.getDescription())
            .transactionDate(transaction.getTransactionDate())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .tags(transaction.getTags())
            .location(transaction.getLocation())
            .receiptUrl(transaction.getReceiptUrl())
            .isRecurring(transaction.getIsRecurring())
            .recurringPattern(transaction.getRecurringPattern())
            .dayOfWeek(transaction.getTransactionDate().getDayOfWeek().getValue())
            .monthOfYear(transaction.getTransactionDate().getMonthValue())
            .year(transaction.getTransactionDate().getYear())
            .timeOfDay(getTimeOfDay(transaction.getCreatedAt()))
            .build();
    }

    private String getTimeOfDay(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        if (hour >= 6 && hour < 12) return "MORNING";
        if (hour >= 12 && hour < 18) return "AFTERNOON";
        if (hour >= 18 && hour < 22) return "EVENING";
        return "NIGHT";
    }

    private void publishTransactionEvent(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSACTION_CREATED");
        event.put("transactionId", transaction.getId());
        event.put("userId", transaction.getUserId());
        event.put("amount", transaction.getAmount());
        event.put("type", transaction.getType());
        event.put("timestamp", LocalDateTime.now());
        
        kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, transaction.getUserId().toString(), transaction);
        kafkaTemplate.send(TRANSACTION_ANALYTICS_TOPIC, transaction.getUserId().toString(), event);
    }

    private void publishTransactionUpdateEvent(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSACTION_UPDATED");
        event.put("transactionId", transaction.getId());
        event.put("userId", transaction.getUserId());
        event.put("timestamp", LocalDateTime.now());
        
        kafkaTemplate.send(TRANSACTION_ANALYTICS_TOPIC, transaction.getUserId().toString(), event);
    }

    private void publishTransactionDeletionEvent(UUID transactionId, UUID userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSACTION_DELETED");
        event.put("transactionId", transactionId);
        event.put("userId", userId);
        event.put("timestamp", LocalDateTime.now());
        
        kafkaTemplate.send(TRANSACTION_ANALYTICS_TOPIC, userId.toString(), event);
    }

    private void evictUserCaches(UUID userId) {
        // Cache eviction would be handled by @CacheEvict annotations in a real implementation
        log.debug("Evicting caches for user: {}", userId);
    }

    // Mapping helper methods for complex query results
    
    private Map<String, Object> mapMonthlyCategorySpending(Object[] row) {
        Map<String, Object> result = new HashMap<>();
        result.put("month", row[0]);
        result.put("categoryName", row[1]);
        result.put("categoryType", row[2]);
        result.put("transactionCount", row[3]);
        result.put("totalAmount", row[4]);
        result.put("avgAmount", row[5]);
        result.put("minAmount", row[6]);
        result.put("maxAmount", row[7]);
        result.put("amountStddev", row[8]);
        result.put("medianAmount", row[9]);
        result.put("monthOverMonthChange", row[10]);
        return result;
    }

    private Map<String, Object> mapCashFlowData(Object[] row) {
        Map<String, Object> result = new HashMap<>();
        result.put("transactionDate", row[0]);
        result.put("dailyIncome", row[1]);
        result.put("dailyExpenses", row[2]);
        result.put("dailyNet", row[3]);
        result.put("cumulativeIncome", row[4]);
        result.put("cumulativeExpenses", row[5]);
        result.put("runningBalance", row[6]);
        result.put("sevenDayAvgNet", row[7]);
        result.put("cashflowStatus", row[8]);
        return result;
    }

    private Map<String, Object> mapSpendingPattern(Object[] row) {
        Map<String, Object> result = new HashMap<>();
        result.put("dayName", row[0]);
        result.put("timePeriod", row[1]);
        result.put("categoryName", row[2]);
        result.put("frequency", row[3]);
        result.put("avgAmount", row[4]);
        result.put("totalAmount", row[5]);
        result.put("amountVariance", row[6]);
        result.put("locationDiversity", row[7]);
        result.put("frequencyRank", row[8]);
        result.put("amountRank", row[9]);
        return result;
    }

    private Map<String, Object> mapBudgetVariance(Object[] row) {
        Map<String, Object> result = new HashMap<>();
        result.put("budgetId", row[0]);
        result.put("budgetName", row[1]);
        result.put("budgetAmount", row[2]);
        result.put("budgetPeriod", row[3]);
        result.put("categoryName", row[4]);
        result.put("actualSpent", row[5]);
        result.put("variance", row[6]);
        result.put("utilizationPercentage", row[7]);
        result.put("transactionCount", row[8]);
        result.put("avgTransactionAmount", row[9]);
        result.put("budgetStatus", row[10]);
        result.put("overspendAmount", row[11]);
        return result;
    }

    private Map<String, Object> mapAnomalousTransaction(Object[] row) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", row[0]);
        result.put("amount", row[1]);
        result.put("description", row[2]);
        result.put("transactionDate", row[3]);
        result.put("createdAt", row[4]);
        result.put("categoryName", row[5]);
        result.put("avgAmount", row[6]);
        result.put("zScore", row[7]);
        result.put("anomalyType", row[8]);
        result.put("amountClassification", row[9]);
        return result;
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .userId(transaction.getUserId())
            .accountId(transaction.getAccountId())
            .categoryId(transaction.getCategoryId())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .description(transaction.getDescription())
            .transactionDate(transaction.getTransactionDate())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .tags(transaction.getTags() != null && !transaction.getTags().isEmpty() ? 
                List.of(transaction.getTags().split(",")) : List.of())
            .location(transaction.getLocation())
            .receiptUrl(transaction.getReceiptUrl())
            .isRecurring(transaction.getIsRecurring())
            .recurringPattern(transaction.getRecurringPattern())
            .version(transaction.getVersion())
            .build();
    }

    private TransactionSearchResponse mapToSearchResponse(TransactionDocument document) {
        return TransactionSearchResponse.builder()
            .id(document.getId())
            .userId(document.getUserId())
            .accountId(document.getAccountId())
            .categoryId(document.getCategoryId())
            .amount(document.getAmount())
            .type(document.getType())
            .description(document.getDescription())
            .transactionDate(document.getTransactionDate() != null ? document.getTransactionDate().toString() : null)
            .createdAt(document.getCreatedAt() != null ? document.getCreatedAt().toString() : null)
            .updatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : null)
            .tags(document.getTags() != null && !document.getTags().isEmpty() ? 
                List.of(document.getTags().split(",")) : List.of())
            .location(document.getLocation())
            .receiptUrl(document.getReceiptUrl())
            .isRecurring(document.getIsRecurring())
            .recurringPattern(document.getRecurringPattern())
            .build();
    }

    private List<Map<String, Object>> getTopCategories(Map<String, Double> categoryTotals, int limit) {
        return categoryTotals.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Map<String, Object> result = new HashMap<>();
                result.put("category", entry.getKey());
                result.put("totalAmount", entry.getValue());
                return result;
            })
            .collect(Collectors.toList());
    }
}