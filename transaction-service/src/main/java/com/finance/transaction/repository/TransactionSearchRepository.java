package com.finance.transaction.repository;

import com.finance.transaction.document.TransactionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Elasticsearch Repository for Transaction Search and Analytics
 * Demonstrates advanced search capabilities with Elasticsearch
 */
@Repository
public interface TransactionSearchRepository extends ElasticsearchRepository<TransactionDocument, String> {
    
    // Basic search methods
    Page<TransactionDocument> findByUserId(UUID userId, Pageable pageable);
    
    Page<TransactionDocument> findByUserIdAndType(UUID userId, String type, Pageable pageable);
    
    Page<TransactionDocument> findByUserIdAndTransactionDateBetween(
        UUID userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    // Advanced search with Elasticsearch queries
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"range": {"amount": {"gte": ?1, "lte": ?2}}}
                ]
            }
        }
        """)
    Page<TransactionDocument> findByUserIdAndAmountRange(
        UUID userId, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);
    
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"multi_match": {
                        "query": "?1",
                        "fields": ["description^2", "location", "tags"],
                        "type": "best_fields",
                        "fuzziness": "AUTO"
                    }}
                ]
            }
        }
        """)
    Page<TransactionDocument> searchByUserIdAndText(UUID userId, String searchText, Pageable pageable);
    
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"terms": {"tags": ?1}}
                ]
            }
        }
        """)
    Page<TransactionDocument> findByUserIdAndTagsIn(UUID userId, List<String> tags, Pageable pageable);
    
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"range": {"transactionDate": {"gte": "?1", "lte": "?2"}}},
                    {"term": {"categoryId": "?3"}}
                ]
            }
        }
        """)
    Page<TransactionDocument> findByUserIdAndDateRangeAndCategory(
        UUID userId, LocalDate startDate, LocalDate endDate, UUID categoryId, Pageable pageable);
    
    // Analytics queries
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"term": {"type": "EXPENSE"}},
                    {"range": {"transactionDate": {"gte": "?1"}}}
                ]
            }
        }
        """)
    List<TransactionDocument> findRecentExpensesByUserId(UUID userId, LocalDate since);
    
    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"userId": "?0"}},
                    {"term": {"isRecurring": true}}
                ]
            }
        }
        """)
    List<TransactionDocument> findRecurringTransactionsByUserId(UUID userId);
}