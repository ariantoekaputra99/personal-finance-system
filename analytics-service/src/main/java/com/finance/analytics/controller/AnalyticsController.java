package com.finance.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.finance.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Analytics Controller with comprehensive REST API
 * Demonstrates analytics and reporting capabilities
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics & Reporting", description = "APIs for financial analytics, reports, and insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
        summary = "Get dashboard data",
        description = "Retrieve comprehensive dashboard data including balance, income, expenses, and trends"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @Parameter(description = "User ID")
            @RequestParam UUID userId) {
        
        Map<String, Object> dashboard = analyticsService.getDashboardData(userId);
        return ResponseEntity.ok(dashboard);
    }

    @Operation(
        summary = "Get financial reports",
        description = "Generate detailed financial reports for specified time period"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getFinancialReports(
            @Parameter(description = "Report type (DAILY, WEEKLY, MONTHLY, YEARLY)")
            @RequestParam(defaultValue = "MONTHLY") String type,
            @Parameter(description = "Start date for report")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @Parameter(description = "End date for report")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            @Parameter(description = "User ID")
            @RequestParam UUID userId) {
        
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        Map<String, Object> report = analyticsService.getFinancialReports(type, start, end, userId);
        return ResponseEntity.ok(report);
    }

    @Operation(
        summary = "Get spending trends",
        description = "Analyze spending trends over time with insights and recommendations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trends data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getSpendingTrends(
            @Parameter(description = "Period for trend analysis (LAST_3_MONTHS, LAST_6_MONTHS, LAST_YEAR)")
            @RequestParam(defaultValue = "LAST_6_MONTHS") String period,
            @Parameter(description = "User ID")
            @RequestParam UUID userId) {
        
        Map<String, Object> trends = analyticsService.getSpendingTrends(period, userId);
        return ResponseEntity.ok(trends);
    }

    @Operation(
        summary = "Get budget analysis",
        description = "Analyze budget performance and provide recommendations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Budget analysis retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/budget-analysis")
    public ResponseEntity<Map<String, Object>> getBudgetAnalysis(
            @Parameter(description = "User ID")
            @RequestParam UUID userId,
            @Parameter(description = "Month for analysis (YYYY-MM format)")
            @RequestParam(required = false) String month) {
        
        Map<String, Object> analysis = analyticsService.getBudgetAnalysis(userId, month);
        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Get income vs expense comparison",
        description = "Compare income and expenses over different time periods"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comparison data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/income-expense-comparison")
    public ResponseEntity<Map<String, Object>> getIncomeExpenseComparison(
            @Parameter(description = "Comparison period (MONTHLY, QUARTERLY, YEARLY)")
            @RequestParam(defaultValue = "MONTHLY") String period,
            @Parameter(description = "Number of periods to compare")
            @RequestParam(defaultValue = "12") int periods,
            @Parameter(description = "User ID")
            @RequestParam UUID userId) {
        
        Map<String, Object> comparison = analyticsService.getIncomeExpenseComparison(period, periods, userId);
        return ResponseEntity.ok(comparison);
    }
}