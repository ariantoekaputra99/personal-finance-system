package com.finance.user.repository;

import com.finance.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository with Advanced Native SQL Queries
 * Demonstrates complex SQL operations and Spring Data JPA
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByIsActiveTrue();
    
    /**
     * Advanced Native SQL Query - Find users with recent activity
     * Demonstrates complex JOIN operations and date filtering
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM users u
        LEFT JOIN transactions t ON u.id = t.user_id
        WHERE u.is_active = true
        AND (
            u.created_at >= :since
            OR t.created_at >= :since
        )
        ORDER BY COALESCE(t.created_at, u.created_at) DESC
        """, nativeQuery = true)
    List<User> findActiveUsersWithRecentActivity(@Param("since") LocalDateTime since);
    
    /**
     * Advanced Native SQL Query - User statistics with aggregations
     * Demonstrates complex aggregation and window functions
     */
    @Query(value = """
        SELECT 
            u.id,
            u.username,
            u.email,
            u.full_name,
            u.created_at,
            COUNT(t.id) as transaction_count,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) as total_income,
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) as total_expenses,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0) as net_worth,
            COUNT(DISTINCT a.id) as account_count,
            RANK() OVER (ORDER BY COUNT(t.id) DESC) as activity_rank
        FROM users u
        LEFT JOIN transactions t ON u.id = t.user_id 
            AND t.created_at >= :startDate 
            AND t.created_at <= :endDate
        LEFT JOIN accounts a ON u.id = a.user_id AND a.is_active = true
        WHERE u.is_active = true
        GROUP BY u.id, u.username, u.email, u.full_name, u.created_at
        ORDER BY transaction_count DESC
        """, nativeQuery = true)
    List<Object[]> getUserStatistics(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * Advanced Native SQL Query - Find users by spending pattern
     * Demonstrates complex analytical queries with CTEs
     */
    @Query(value = """
        WITH user_spending_patterns AS (
            SELECT 
                u.id,
                u.username,
                AVG(t.amount) as avg_transaction_amount,
                COUNT(t.id) as transaction_frequency,
                EXTRACT(DOW FROM t.transaction_date) as preferred_day,
                COUNT(*) OVER (PARTITION BY u.id, EXTRACT(DOW FROM t.transaction_date)) as day_frequency
            FROM users u
            JOIN transactions t ON u.id = t.user_id
            WHERE t.type = 'EXPENSE'
            AND t.created_at >= :startDate
            GROUP BY u.id, u.username, EXTRACT(DOW FROM t.transaction_date)
        ),
        user_preferences AS (
            SELECT 
                id,
                username,
                avg_transaction_amount,
                transaction_frequency,
                preferred_day,
                ROW_NUMBER() OVER (PARTITION BY id ORDER BY day_frequency DESC) as day_rank
            FROM user_spending_patterns
        )
        SELECT DISTINCT
            up.id,
            up.username,
            up.avg_transaction_amount,
            up.transaction_frequency,
            up.preferred_day as most_active_day
        FROM user_preferences up
        WHERE up.day_rank = 1
        AND up.avg_transaction_amount BETWEEN :minAmount AND :maxAmount
        ORDER BY up.transaction_frequency DESC
        """, nativeQuery = true)
    List<Object[]> findUsersBySpendingPattern(@Param("startDate") LocalDateTime startDate,
                                            @Param("minAmount") Double minAmount,
                                            @Param("maxAmount") Double maxAmount);
    
    /**
     * Advanced Native SQL Query - User cohort analysis
     * Demonstrates advanced analytics with date functions
     */
    @Query(value = """
        WITH user_cohorts AS (
            SELECT 
                u.id,
                u.username,
                u.created_at as registration_date,
                DATE_TRUNC('month', u.created_at) as cohort_month,
                MIN(t.created_at) as first_transaction_date,
                COUNT(DISTINCT DATE_TRUNC('month', t.created_at)) as active_months,
                EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - u.created_at))/86400 as days_since_registration
            FROM users u
            LEFT JOIN transactions t ON u.id = t.user_id
            WHERE u.created_at >= :cohortStartDate
            GROUP BY u.id, u.username, u.created_at
        )
        SELECT 
            cohort_month,
            COUNT(*) as cohort_size,
            COUNT(first_transaction_date) as activated_users,
            ROUND(COUNT(first_transaction_date)::numeric / COUNT(*)::numeric * 100, 2) as activation_rate,
            ROUND(AVG(active_months), 2) as avg_active_months,
            ROUND(AVG(days_since_registration), 0) as avg_days_since_registration
        FROM user_cohorts
        GROUP BY cohort_month
        ORDER BY cohort_month DESC
        """, nativeQuery = true)
    List<Object[]> getUserCohortAnalysis(@Param("cohortStartDate") LocalDateTime cohortStartDate);
}