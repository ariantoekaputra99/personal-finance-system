package com.finance.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getDashboardData(UUID userId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get total balance (sum of all transactions)
        String balanceQuery = "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) FROM transactions WHERE user_id = ?";
        BigDecimal totalBalance = jdbcTemplate.queryForObject(balanceQuery, BigDecimal.class, userId);
        
        // Get monthly income and expense
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        String monthlyIncomeQuery = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND type = 'INCOME' AND transaction_date >= ?";
        String monthlyExpenseQuery = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND type = 'EXPENSE' AND transaction_date >= ?";
        
        BigDecimal monthlyIncome = jdbcTemplate.queryForObject(monthlyIncomeQuery, BigDecimal.class, userId, startOfMonth);
        BigDecimal monthlyExpense = jdbcTemplate.queryForObject(monthlyExpenseQuery, BigDecimal.class, userId, startOfMonth);
        
        double savingsRate = monthlyIncome.doubleValue() > 0 ? 
            ((monthlyIncome.doubleValue() - monthlyExpense.doubleValue()) / monthlyIncome.doubleValue()) * 100 : 0;
        
        dashboard.put("totalBalance", totalBalance);
        dashboard.put("monthlyIncome", monthlyIncome);
        dashboard.put("monthlyExpense", monthlyExpense);
        dashboard.put("savingsRate", Math.round(savingsRate * 100.0) / 100.0);
        
        // Get top categories
        String topCategoriesQuery = """
            SELECT category_id, SUM(amount) as total_amount 
            FROM transactions 
            WHERE user_id = ? AND type = 'EXPENSE' AND transaction_date >= ? 
            GROUP BY category_id 
            ORDER BY total_amount DESC 
            LIMIT 5
        """;
        
        List<Map<String, Object>> topCategories = jdbcTemplate.query(topCategoriesQuery, 
            (rs, rowNum) -> {
                Map<String, Object> category = new HashMap<>();
                category.put("category", rs.getString("category_id"));
                category.put("amount", rs.getBigDecimal("total_amount"));
                double percentage = monthlyExpense.doubleValue() > 0 ? 
                    (rs.getBigDecimal("total_amount").doubleValue() / monthlyExpense.doubleValue()) * 100 : 0;
                category.put("percentage", Math.round(percentage * 100.0) / 100.0);
                return category;
            }, userId, startOfMonth);
        
        dashboard.put("topCategories", topCategories);
        return dashboard;
    }

    public Map<String, Object> getFinancialReports(String type, LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        Map<String, Object> report = new HashMap<>();
        report.put("period", startDate.toLocalDate() + " to " + endDate.toLocalDate());
        report.put("type", type);
        
        // Summary data
        String incomeQuery = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND type = 'INCOME' AND created_at BETWEEN ? AND ?";
        String expenseQuery = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND type = 'EXPENSE' AND created_at BETWEEN ? AND ?";
        
        BigDecimal totalIncome = jdbcTemplate.queryForObject(incomeQuery, BigDecimal.class, userId, startDate, endDate);
        BigDecimal totalExpense = jdbcTemplate.queryForObject(expenseQuery, BigDecimal.class, userId, startDate, endDate);
        BigDecimal netIncome = totalIncome.subtract(totalExpense);
        
        double savingsRate = totalIncome.doubleValue() > 0 ? 
            (netIncome.doubleValue() / totalIncome.doubleValue()) * 100 : 0;
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netIncome", netIncome);
        summary.put("savingsRate", Math.round(savingsRate * 100.0) / 100.0);
        report.put("summary", summary);
        
        // Category analysis
        String categoryQuery = """
            SELECT category_id, SUM(amount) as total_amount, COUNT(*) as transaction_count, AVG(amount) as avg_amount
            FROM transactions 
            WHERE user_id = ? AND type = 'EXPENSE' AND created_at BETWEEN ? AND ?
            GROUP BY category_id 
            ORDER BY total_amount DESC
        """;
        
        List<Map<String, Object>> categoryAnalysis = jdbcTemplate.query(categoryQuery,
            (rs, rowNum) -> {
                Map<String, Object> category = new HashMap<>();
                category.put("category", rs.getString("category_id"));
                category.put("amount", rs.getBigDecimal("total_amount"));
                category.put("transactionCount", rs.getInt("transaction_count"));
                category.put("averageAmount", rs.getBigDecimal("avg_amount"));
                double percentage = totalExpense.doubleValue() > 0 ? 
                    (rs.getBigDecimal("total_amount").doubleValue() / totalExpense.doubleValue()) * 100 : 0;
                category.put("percentage", Math.round(percentage * 100.0) / 100.0);
                return category;
            }, userId, startDate, endDate);
        
        report.put("categoryAnalysis", categoryAnalysis);
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("incomeGrowth", 0.0);
        trends.put("expenseGrowth", 0.0);
        report.put("trends", trends);
        
        return report;
    }

    public Map<String, Object> getSpendingTrends(String period, UUID userId) {
        Map<String, Object> trends = new HashMap<>();
        trends.put("period", period);
        
        int months = switch (period) {
            case "LAST_3_MONTHS" -> 3;
            case "LAST_YEAR" -> 12;
            default -> 6;
        };
        
        String trendsQuery = """
            SELECT 
                DATE_TRUNC('month', transaction_date) as month,
                SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as income,
                SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expense
            FROM transactions 
            WHERE user_id = ? AND transaction_date >= ?
            GROUP BY DATE_TRUNC('month', transaction_date)
            ORDER BY month DESC
            LIMIT ?
        """;
        
        LocalDate startDate = LocalDate.now().minusMonths(months);
        List<Map<String, Object>> data = jdbcTemplate.query(trendsQuery,
            (rs, rowNum) -> {
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", rs.getDate("month").toLocalDate().toString());
                BigDecimal income = rs.getBigDecimal("income");
                BigDecimal expense = rs.getBigDecimal("expense");
                monthData.put("income", income);
                monthData.put("expense", expense);
                monthData.put("savings", income.subtract(expense));
                return monthData;
            }, userId, startDate, months);
        
        trends.put("data", data);
        
        List<String> insights = Arrays.asList(
            "Analysis based on your actual transaction data",
            "Track your spending patterns over time",
            "Monitor your savings rate monthly"
        );
        trends.put("insights", insights);
        
        return trends;
    }

    public Map<String, Object> getBudgetAnalysis(UUID userId, String month) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("month", month != null ? month : LocalDate.now().toString().substring(0, 7));
        
        // Since we don't have budgets table, return analysis based on spending patterns
        LocalDate startDate = month != null ? 
            LocalDate.parse(month + "-01") : LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        String categorySpendingQuery = """
            SELECT category_id, SUM(amount) as spent, COUNT(*) as transaction_count
            FROM transactions 
            WHERE user_id = ? AND type = 'EXPENSE' AND transaction_date BETWEEN ? AND ?
            GROUP BY category_id 
            ORDER BY spent DESC
        """;
        
        List<Map<String, Object>> categoryBudgets = jdbcTemplate.query(categorySpendingQuery,
            (rs, rowNum) -> {
                Map<String, Object> category = new HashMap<>();
                String categoryId = rs.getString("category_id");
                BigDecimal spent = rs.getBigDecimal("spent");
                
                category.put("category", categoryId);
                category.put("spent", spent);
                category.put("transactionCount", rs.getInt("transaction_count"));
                category.put("status", "NO_BUDGET_SET");
                return category;
            }, userId, startDate, endDate);
        
        analysis.put("categoryBudgets", categoryBudgets);
        
        BigDecimal totalSpent = categoryBudgets.stream()
            .map(cat -> (BigDecimal) cat.get("spent"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> overall = new HashMap<>();
        overall.put("totalSpent", totalSpent);
        overall.put("categoryCount", categoryBudgets.size());
        analysis.put("overall", overall);
        
        List<String> recommendations = Arrays.asList(
            "Set up budgets for your spending categories to track your progress",
            "Monitor your spending patterns to identify areas for improvement",
            "Consider using the 50/30/20 rule: 50% needs, 30% wants, 20% savings"
        );
        analysis.put("recommendations", recommendations);
        
        return analysis;
    }

    public Map<String, Object> getIncomeExpenseComparison(String period, int periods, UUID userId) {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("period", period);
        comparison.put("periods", periods);
        
        String dateFormat = switch (period) {
            case "QUARTERLY" -> "quarter";
            case "YEARLY" -> "year";
            default -> "month";
        };
        
        String comparisonQuery = """
            SELECT 
                DATE_TRUNC('%s', transaction_date) as period_date,
                SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as income,
                SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expense
            FROM transactions 
            WHERE user_id = ? AND transaction_date >= ?
            GROUP BY DATE_TRUNC('%s', transaction_date)
            ORDER BY period_date DESC
            LIMIT ?
        """.formatted(dateFormat, dateFormat);
        
        LocalDate startDate = switch (period) {
            case "QUARTERLY" -> LocalDate.now().minusMonths(periods * 3);
            case "YEARLY" -> LocalDate.now().minusYears(periods);
            default -> LocalDate.now().minusMonths(periods);
        };
        
        List<Map<String, Object>> data = jdbcTemplate.query(comparisonQuery,
            (rs, rowNum) -> {
                Map<String, Object> periodData = new HashMap<>();
                periodData.put("period", rs.getDate("period_date").toLocalDate().toString());
                BigDecimal income = rs.getBigDecimal("income");
                BigDecimal expense = rs.getBigDecimal("expense");
                periodData.put("income", income);
                periodData.put("expense", expense);
                periodData.put("netIncome", income.subtract(expense));
                return periodData;
            }, userId, startDate, periods);
        
        comparison.put("data", data);
        
        // Calculate summary statistics
        if (!data.isEmpty()) {
            double avgIncome = data.stream()
                .mapToDouble(d -> ((BigDecimal) d.get("income")).doubleValue())
                .average().orElse(0.0);
            double avgExpense = data.stream()
                .mapToDouble(d -> ((BigDecimal) d.get("expense")).doubleValue())
                .average().orElse(0.0);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("averageIncome", BigDecimal.valueOf(avgIncome));
            summary.put("averageExpense", BigDecimal.valueOf(avgExpense));
            summary.put("averageNetIncome", BigDecimal.valueOf(avgIncome - avgExpense));
            comparison.put("summary", summary);
        }
        
        return comparison;
    }
}