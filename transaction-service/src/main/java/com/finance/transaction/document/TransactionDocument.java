package com.finance.transaction.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Elasticsearch Document for Transaction Search and Analytics
 * Demonstrates Elasticsearch integration with Spring Data
 */
@Document(indexName = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private UUID userId;
    
    @Field(type = FieldType.Keyword)
    private UUID accountId;
    
    @Field(type = FieldType.Keyword)
    private String categoryId;
    
    @Field(type = FieldType.Double)
    private BigDecimal amount;
    
    @Field(type = FieldType.Keyword)
    private String type;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Date)
    private LocalDate transactionDate;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Keyword)
    private String tags;
    
    @Field(type = FieldType.Text)
    private String location;
    
    @Field(type = FieldType.Keyword)
    private String receiptUrl;
    
    @Field(type = FieldType.Boolean)
    private Boolean isRecurring;
    
    @Field(type = FieldType.Keyword)
    private String recurringPattern;
    
    // Additional fields for analytics
    @Field(type = FieldType.Keyword)
    private String categoryName;
    
    @Field(type = FieldType.Keyword)
    private String accountName;
    
    @Field(type = FieldType.Integer)
    private Integer dayOfWeek;
    
    @Field(type = FieldType.Integer)
    private Integer monthOfYear;
    
    @Field(type = FieldType.Integer)
    private Integer year;
    
    @Field(type = FieldType.Keyword)
    private String timeOfDay; // MORNING, AFTERNOON, EVENING, NIGHT
}