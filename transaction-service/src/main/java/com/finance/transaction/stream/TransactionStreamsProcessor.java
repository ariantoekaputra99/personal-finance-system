package com.finance.transaction.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Streams Processor for Real-time Transaction Processing
 * Demonstrates advanced stream processing and analytics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionStreamsProcessor {

    private final ObjectMapper objectMapper;

    public static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    public static final String TRANSACTION_ANALYTICS_TOPIC = "transaction-analytics";
    public static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";
    public static final String BUDGET_ALERTS_TOPIC = "budget-alerts";

    /**
     * Configure Kafka Streams topology for transaction processing
     */
    @Autowired
    public void configureStreams(StreamsBuilder streamsBuilder) {
        
        JsonSerde<Transaction> transactionSerde = new JsonSerde<>(Transaction.class, objectMapper);
        JsonSerde<Map<String, Object>> mapSerde = new JsonSerde<>(Map.class, objectMapper);
        
        KStream<String, Transaction> transactionStream = streamsBuilder
            .stream(TRANSACTION_EVENTS_TOPIC, Consumed.with(Serdes.String(), transactionSerde));

        Map<String, KStream<String, Transaction>> branches = transactionStream
            .split(Named.as("transaction-"))
            .branch((key, transaction) -> "EXPENSE".equals(transaction.getType()),
                   Branched.as("expenses"))
            .branch((key, transaction) -> "INCOME".equals(transaction.getType()),
                   Branched.as("income"))
            .defaultBranch(Branched.as("transfers"));

        processExpenseTransactions(branches.get("transaction-expenses"), mapSerde);
        processIncomeTransactions(branches.get("transaction-income"), mapSerde);
        createSpendingAnalytics(transactionStream, mapSerde);
        createBudgetMonitoring(branches.get("transaction-expenses"), mapSerde);
    }

    private void processExpenseTransactions(KStream<String, Transaction> expenseStream, 
                                          JsonSerde<Map<String, Object>> mapSerde) {
        
        KStream<String, Map<String, Object>> fraudAlerts = expenseStream
            .filter((key, transaction) -> isSuspiciousTransaction(transaction))
            .mapValues(transaction -> {
                Map<String, Object> alert = new HashMap<>();
                alert.put("alertType", "SUSPICIOUS_TRANSACTION");
                alert.put("userId", transaction.getUserId().toString());
                alert.put("transactionId", transaction.getId().toString());
                alert.put("amount", transaction.getAmount());
                alert.put("description", transaction.getDescription());
                alert.put("timestamp", LocalDateTime.now());
                alert.put("riskLevel", calculateRiskLevel(transaction));
                return alert;
            });
        
        fraudAlerts.to(FRAUD_ALERTS_TOPIC, Produced.with(Serdes.String(), mapSerde));
    }

    private void processIncomeTransactions(KStream<String, Transaction> incomeStream,
                                         JsonSerde<Map<String, Object>> mapSerde) {
        
        KStream<String, Map<String, Object>> incomeAnalytics = incomeStream
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(30)))
            .aggregate(
                () -> {
                    Map<String, Object> initial = new HashMap<>();
                    initial.put("totalIncome", BigDecimal.ZERO);
                    initial.put("transactionCount", 0L);
                    initial.put("sources", new HashMap<>());
                    return initial;
                },
                (key, transaction, aggregate) -> aggregateIncomeData(transaction, aggregate),
                Materialized.with(Serdes.String(), mapSerde)
            )
            .toStream()
            .map((windowedKey, aggregate) -> {
                Map<String, Object> result = new HashMap<>();
                result.put("analyticsType", "MONTHLY_INCOME_SUMMARY");
                result.put("userId", windowedKey.key());
                result.put("period", windowedKey.window().startTime());
                result.put("data", aggregate);
                result.put("timestamp", LocalDateTime.now());
                return KeyValue.pair(windowedKey.key(), result);
            });
        
        incomeAnalytics.to(TRANSACTION_ANALYTICS_TOPIC, Produced.with(Serdes.String(), mapSerde));
    }

    private void createSpendingAnalytics(KStream<String, Transaction> transactionStream,
                                       JsonSerde<Map<String, Object>> mapSerde) {
        
        KStream<String, Map<String, Object>> dailySpending = transactionStream
            .filter((key, transaction) -> "EXPENSE".equals(transaction.getType()))
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
            .aggregate(
                () -> createEmptySpendingAggregate(),
                (key, transaction, aggregate) -> aggregateSpendingData(transaction, aggregate),
                Materialized.with(Serdes.String(), mapSerde)
            )
            .toStream()
            .map((windowedKey, aggregate) -> {
                Map<String, Object> result = new HashMap<>();
                result.put("analyticsType", "DAILY_SPENDING_SUMMARY");
                result.put("userId", windowedKey.key());
                result.put("date", windowedKey.window().startTime());
                result.put("data", aggregate);
                result.put("timestamp", LocalDateTime.now());
                return KeyValue.pair(windowedKey.key(), result);
            });
        
        dailySpending.to(TRANSACTION_ANALYTICS_TOPIC, Produced.with(Serdes.String(), mapSerde));
    }

    private void createBudgetMonitoring(KStream<String, Transaction> expenseStream,
                                      JsonSerde<Map<String, Object>> mapSerde) {
        
        KStream<String, Map<String, Object>> budgetAlerts = expenseStream
            .filter((key, transaction) -> transaction.getCategoryId() != null)
            .selectKey((key, transaction) -> 
                transaction.getUserId() + ":" + transaction.getCategoryId())
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(30)))
            .aggregate(
                () -> {
                    Map<String, Object> initial = new HashMap<>();
                    initial.put("totalSpent", BigDecimal.ZERO);
                    initial.put("transactionCount", 0L);
                    return initial;
                },
                (key, transaction, aggregate) -> {
                    BigDecimal currentTotal = (BigDecimal) aggregate.get("totalSpent");
                    Long currentCount = (Long) aggregate.get("transactionCount");
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("totalSpent", currentTotal.add(transaction.getAmount()));
                    result.put("transactionCount", currentCount + 1);
                    return result;
                },
                Materialized.with(Serdes.String(), mapSerde)
            )
            .toStream()
            .filter((windowedKey, aggregate) -> {
                BigDecimal totalSpent = (BigDecimal) aggregate.get("totalSpent");
                return totalSpent.compareTo(new BigDecimal("1000000")) > 0;
            })
            .map((windowedKey, aggregate) -> {
                String[] keyParts = windowedKey.key().split(":");
                Map<String, Object> alert = new HashMap<>();
                alert.put("alertType", "BUDGET_THRESHOLD_EXCEEDED");
                alert.put("userId", keyParts[0]);
                alert.put("categoryId", keyParts[1]);
                alert.put("period", windowedKey.window().startTime());
                alert.put("totalSpent", aggregate.get("totalSpent"));
                alert.put("transactionCount", aggregate.get("transactionCount"));
                alert.put("timestamp", LocalDateTime.now());
                return KeyValue.pair(keyParts[0], alert);
            });
        
        budgetAlerts.to(BUDGET_ALERTS_TOPIC, Produced.with(Serdes.String(), mapSerde));
    }

    private boolean isSuspiciousTransaction(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        
        if (amount.compareTo(new BigDecimal("10000000")) > 0) {
            return true;
        }
        
        String description = transaction.getDescription();
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            return lowerDesc.contains("test") || 
                   lowerDesc.contains("fake") || 
                   lowerDesc.contains("dummy");
        }
        
        return false;
    }

    private String calculateRiskLevel(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        
        if (amount.compareTo(new BigDecimal("50000000")) > 0) {
            return "CRITICAL";
        } else if (amount.compareTo(new BigDecimal("10000000")) > 0) {
            return "HIGH";
        } else if (amount.compareTo(new BigDecimal("5000000")) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private Map<String, Object> aggregateIncomeData(Transaction transaction, Map<String, Object> aggregate) {
        BigDecimal currentTotal = (BigDecimal) aggregate.get("totalIncome");
        Long currentCount = (Long) aggregate.get("transactionCount");
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", currentTotal.add(transaction.getAmount()));
        result.put("transactionCount", currentCount + 1);
        result.put("sources", aggregate.get("sources"));
        return result;
    }

    private Map<String, Object> createEmptySpendingAggregate() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", BigDecimal.ZERO);
        result.put("transactionCount", 0L);
        result.put("categories", new HashMap<>());
        result.put("avgAmount", BigDecimal.ZERO);
        result.put("maxAmount", BigDecimal.ZERO);
        result.put("minAmount", BigDecimal.ZERO);
        return result;
    }

    private Map<String, Object> aggregateSpendingData(Transaction transaction, Map<String, Object> aggregate) {
        BigDecimal currentTotal = (BigDecimal) aggregate.get("totalAmount");
        Long currentCount = (Long) aggregate.get("transactionCount");
        BigDecimal newTotal = currentTotal.add(transaction.getAmount());
        Long newCount = currentCount + 1;
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", newTotal);
        result.put("transactionCount", newCount);
        result.put("categories", aggregate.get("categories"));
        result.put("avgAmount", newTotal.divide(BigDecimal.valueOf(newCount), 2, BigDecimal.ROUND_HALF_UP));
        result.put("maxAmount", transaction.getAmount().max((BigDecimal) aggregate.get("maxAmount")));
        result.put("minAmount", currentCount == 0 ? transaction.getAmount() : 
                   transaction.getAmount().min((BigDecimal) aggregate.get("minAmount")));
        return result;
    }
}