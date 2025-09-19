package com.finance.transaction.repository;

import com.finance.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transaction Repository with Advanced Native SQL Queries
 * Demonstrates complex financial analytics and reporting queries
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findByUserIdOrderByTransactionDateDesc(UUID userId);
    
    Page<Transaction> findByUserIdOrderByTransactionDateDesc(UUID userId, Pageable pageable);
    
    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDesc(UUID userId, String type);
    
    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
        UUID userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Advanced Native SQL Query - Monthly spending summary with categories
     * Demonstrates complex aggregation and window functions
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', t.transaction_date) as month,
            t.category_id as category_name,
            t.type as category_type,
            COUNT(t.id) as transaction_count,
            SUM(t.amount) as total_amount,
            AVG(t.amount) as avg_amount,
            MIN(t.amount) as min_amount,
            MAX(t.amount) as max_amount,
            STDDEV(t.amount) as amount_stddev,
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY t.amount) as median_amount,
            SUM(t.amount) - LAG(SUM(t.amount)) OVER (
                PARTITION BY t.category_id ORDER BY DATE_TRUNC('month', t.transaction_date)
            ) as month_over_month_change
        FROM transactions t
        WHERE t.user_id = :userId
        AND t.transaction_date >= :startDate
        AND t.transaction_date <= :endDate
        GROUP BY DATE_TRUNC('month', t.transaction_date), t.category_id, t.type
        ORDER BY month DESC, total_amount DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyCategorySpending(@Param("userId") UUID userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * Advanced Native SQL Query - Cash flow analysis with running totals
     * Demonstrates window functions and complex calculations
     */
    @Query(value = """
        WITH daily_cashflow AS (
            SELECT 
                t.transaction_date,
                SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END) as daily_income,
                SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END) as daily_expenses,
                SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END) as daily_net
            FROM transactions t
            WHERE t.user_id = :userId
            AND t.transaction_date >= :startDate
            AND t.transaction_date <= :endDate
            GROUP BY t.transaction_date
        ),
        running_totals AS (
            SELECT 
                transaction_date,
                daily_income,
                daily_expenses,
                daily_net,
                SUM(daily_income) OVER (ORDER BY transaction_date) as cumulative_income,
                SUM(daily_expenses) OVER (ORDER BY transaction_date) as cumulative_expenses,
                SUM(daily_net) OVER (ORDER BY transaction_date) as running_balance,
                AVG(daily_net) OVER (
                    ORDER BY transaction_date 
                    ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
                ) as seven_day_avg_net
            FROM daily_cashflow
        )
        SELECT 
            transaction_date,
            daily_income,
            daily_expenses,
            daily_net,
            cumulative_income,
            cumulative_expenses,
            running_balance,
            seven_day_avg_net,
            CASE 
                WHEN daily_net > seven_day_avg_net * 1.5 THEN 'HIGH_POSITIVE'
                WHEN daily_net > 0 THEN 'POSITIVE'
                WHEN daily_net > seven_day_avg_net * 1.5 THEN 'HIGH_NEGATIVE'
                ELSE 'NEGATIVE'
            END as cashflow_status
        FROM running_totals
        ORDER BY transaction_date DESC
        """, nativeQuery = true)
    List<Object[]> getCashFlowAnalysis(@Param("userId") UUID userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
    
    /**
     * Advanced Native SQL Query - Spending pattern analysis
     * Demonstrates complex analytical queries with behavioral insights
     */
    @Query(value = """
        WITH spending_patterns AS (
            SELECT 
                t.user_id,
                EXTRACT(DOW FROM t.transaction_date) as day_of_week,
                EXTRACT(HOUR FROM t.created_at) as hour_of_day,
                t.category_id as category_name,
                t.amount,
                t.location,
                CASE 
                    WHEN EXTRACT(HOUR FROM t.created_at) BETWEEN 6 AND 11 THEN 'MORNING'
                    WHEN EXTRACT(HOUR FROM t.created_at) BETWEEN 12 AND 17 THEN 'AFTERNOON'
                    WHEN EXTRACT(HOUR FROM t.created_at) BETWEEN 18 AND 22 THEN 'EVENING'
                    ELSE 'NIGHT'
                END as time_period
            FROM transactions t
            WHERE t.user_id = :userId
            AND t.type = 'EXPENSE'
            AND t.transaction_date >= :startDate
        ),
        pattern_analysis AS (
            SELECT 
                day_of_week,
                time_period,
                category_name,
                COUNT(*) as frequency,
                AVG(amount) as avg_amount,
                SUM(amount) as total_amount,
                STDDEV(amount) as amount_variance,
                COUNT(DISTINCT location) as location_diversity
            FROM spending_patterns
            GROUP BY day_of_week, time_period, category_name
        )
        SELECT 
            CASE day_of_week
                WHEN 0 THEN 'Sunday'
                WHEN 1 THEN 'Monday'
                WHEN 2 THEN 'Tuesday'
                WHEN 3 THEN 'Wednesday'
                WHEN 4 THEN 'Thursday'
                WHEN 5 THEN 'Friday'
                WHEN 6 THEN 'Saturday'
            END as day_name,
            time_period,
            category_name,
            frequency,
            avg_amount,
            total_amount,
            amount_variance,
            location_diversity,
            RANK() OVER (PARTITION BY day_of_week ORDER BY frequency DESC) as frequency_rank,
            RANK() OVER (PARTITION BY time_period ORDER BY total_amount DESC) as amount_rank
        FROM pattern_analysis
        WHERE frequency >= :minFrequency
        ORDER BY day_of_week, frequency DESC
        """, nativeQuery = true)
    List<Object[]> getSpendingPatterns(@Param("userId") UUID userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("minFrequency") Integer minFrequency);
    
    /**
     * Advanced Native SQL Query - Budget variance analysis
     * Demonstrates complex financial calculations and comparisons
     */
    @Query(value = """
        WITH budget_comparison AS (
            SELECT 
                b.id as budget_id,
                b.name as budget_name,
                b.amount as budget_amount,
                b.period as budget_period,
                c.name as category_name,
                COALESCE(SUM(t.amount), 0) as actual_spent,
                b.amount - COALESCE(SUM(t.amount), 0) as variance,
                CASE 
                    WHEN b.amount > 0 THEN 
                        (COALESCE(SUM(t.amount), 0) / b.amount) * 100
                    ELSE 0 
                END as utilization_percentage,
                COUNT(t.id) as transaction_count,
                AVG(t.amount) as avg_transaction_amount
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            LEFT JOIN transactions t ON (
                t.category_id = b.category_id 
                AND t.user_id = b.user_id
                AND t.type = 'EXPENSE'
                AND t.transaction_date >= b.start_date
                AND t.transaction_date <= b.end_date
            )
            WHERE b.user_id = :userId
            AND b.is_active = true
            AND b.start_date <= :currentDate
            AND b.end_date >= :currentDate
            GROUP BY b.id, b.name, b.amount, b.period, c.name
        )
        SELECT 
            budget_id,
            budget_name,
            budget_amount,
            budget_period,
            category_name,
            actual_spent,
            variance,
            utilization_percentage,
            transaction_count,
            avg_transaction_amount,
            CASE 
                WHEN utilization_percentage > 100 THEN 'OVER_BUDGET'
                WHEN utilization_percentage > 80 THEN 'NEAR_LIMIT'
                WHEN utilization_percentage > 50 THEN 'ON_TRACK'
                ELSE 'UNDER_UTILIZED'
            END as budget_status,
            CASE 
                WHEN variance < 0 THEN ABS(variance)
                ELSE 0
            END as overspend_amount
        FROM budget_comparison
        ORDER BY utilization_percentage DESC
        """, nativeQuery = true)
    List<Object[]> getBudgetVarianceAnalysis(@Param("userId") UUID userId,
                                           @Param("currentDate") LocalDate currentDate);
    
    /**
     * Advanced Native SQL Query - Anomaly detection for unusual transactions
     * Demonstrates statistical analysis and outlier detection
     */
    @Query(value = """
        WITH user_stats AS (
            SELECT 
                category_id,
                AVG(amount) as avg_amount,
                STDDEV(amount) as stddev_amount,
                PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY amount) as q1,
                PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY amount) as q3,
                COUNT(*) as transaction_count
            FROM transactions
            WHERE user_id = :userId
            AND type = 'EXPENSE'
            AND transaction_date >= :startDate
            GROUP BY category_id
            HAVING COUNT(*) >= 5  -- Minimum transactions for statistical significance
        ),
        anomaly_detection AS (
            SELECT 
                t.id,
                t.amount,
                t.description,
                t.transaction_date,
                t.created_at,
                c.name as category_name,
                s.avg_amount,
                s.stddev_amount,
                s.q1,
                s.q3,
                s.q1 - 1.5 * (s.q3 - s.q1) as lower_bound,
                s.q3 + 1.5 * (s.q3 - s.q1) as upper_bound,
                ABS(t.amount - s.avg_amount) / NULLIF(s.stddev_amount, 0) as z_score
            FROM transactions t
            JOIN user_stats s ON t.category_id = s.category_id
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = :userId
            AND t.type = 'EXPENSE'
            AND t.transaction_date >= :startDate
        )
        SELECT 
            id,
            amount,
            description,
            transaction_date,
            created_at,
            category_name,
            avg_amount,
            z_score,
            CASE 
                WHEN amount > upper_bound OR amount < lower_bound THEN 'IQR_OUTLIER'
                WHEN ABS(z_score) > 3 THEN 'EXTREME_OUTLIER'
                WHEN ABS(z_score) > 2 THEN 'MODERATE_OUTLIER'
                ELSE 'NORMAL'
            END as anomaly_type,
            CASE 
                WHEN amount > avg_amount * 3 THEN 'UNUSUALLY_HIGH'
                WHEN amount < avg_amount * 0.1 THEN 'UNUSUALLY_LOW'
                ELSE 'WITHIN_RANGE'
            END as amount_classification
        FROM anomaly_detection
        WHERE (amount > upper_bound OR amount < lower_bound OR ABS(z_score) > 2)
        ORDER BY ABS(z_score) DESC, transaction_date DESC
        """, nativeQuery = true)
    List<Object[]> detectAnomalousTransactions(@Param("userId") UUID userId,
                                             @Param("startDate") LocalDate startDate);
}