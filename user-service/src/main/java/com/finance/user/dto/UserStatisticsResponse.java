package com.finance.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserStatisticsResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
    private Long transactionCount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netWorth;
    private Long accountCount;
    private Long activityRank;
}