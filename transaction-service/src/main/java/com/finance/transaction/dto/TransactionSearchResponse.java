package com.finance.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSearchResponse {
    private String id;
    private UUID userId;
    private UUID accountId;
    private String categoryId;
    private BigDecimal amount;
    private String type;
    private String description;
    private String transactionDate;
    private String createdAt;
    private String updatedAt;
    private List<String> tags;
    private String location;
    private String receiptUrl;
    private Boolean isRecurring;
    private String recurringPattern;
}