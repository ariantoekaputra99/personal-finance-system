package com.finance.transaction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.finance.transaction.repository")
public class PostgreSQLConfig {
    // Configuration for PostgreSQL array handling is handled by Hibernate automatically
}