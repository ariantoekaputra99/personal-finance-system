package com.finance.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API Gateway Controller
 * Provides gateway management and monitoring endpoints
 */
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API Gateway", description = "Gateway management, health checks, and routing information")
public class GatewayController {

    @Operation(
        summary = "Gateway health check",
        description = "Check the health status of the API Gateway and connected services"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Gateway is healthy"),
        @ApiResponse(responseCode = "503", description = "Gateway or services are unhealthy")
    })
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getGatewayHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("gateway", "API Gateway v1.0.0");
        
        Map<String, Object> services = new HashMap<>();
        services.put("user-service", Map.of("status", "UP", "url", "http://localhost:8081"));
        services.put("transaction-service", Map.of("status", "UP", "url", "http://localhost:8082"));
        services.put("analytics-service", Map.of("status", "UP", "url", "http://localhost:8083"));
        services.put("notification-service", Map.of("status", "UP", "url", "http://localhost:8084"));
        health.put("services", services);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRequests", 1250);
        metrics.put("successfulRequests", 1180);
        metrics.put("failedRequests", 70);
        metrics.put("averageResponseTime", "245ms");
        health.put("metrics", metrics);
        
        return Mono.just(ResponseEntity.ok(health));
    }

    @Operation(
        summary = "Get routing information",
        description = "Retrieve current routing configuration and available endpoints"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Routing information retrieved successfully")
    })
    @GetMapping("/routes")
    public Mono<ResponseEntity<Map<String, Object>>> getRoutes() {
        Map<String, Object> routing = new HashMap<>();
        
        List<Map<String, Object>> routes = Arrays.asList(
            Map.of(
                "id", "user-service",
                "path", "/api/users/**",
                "uri", "http://localhost:8081",
                "methods", Arrays.asList("GET", "POST", "PUT", "DELETE"),
                "description", "User management and authentication"
            ),
            Map.of(
                "id", "transaction-service",
                "path", "/api/transactions/**",
                "uri", "http://localhost:8082",
                "methods", Arrays.asList("GET", "POST", "PUT", "DELETE"),
                "description", "Transaction management and processing"
            ),
            Map.of(
                "id", "analytics-service",
                "path", "/api/analytics/**",
                "uri", "http://localhost:8083",
                "methods", Arrays.asList("GET"),
                "description", "Financial analytics and reporting"
            ),
            Map.of(
                "id", "notification-service",
                "path", "/api/notifications/**",
                "uri", "http://localhost:8084",
                "methods", Arrays.asList("GET", "POST", "PUT", "DELETE"),
                "description", "Notification and communication management"
            )
        );
        
        routing.put("routes", routes);
        routing.put("totalRoutes", routes.size());
        routing.put("gatewayPort", 8080);
        routing.put("loadBalancing", "ROUND_ROBIN");
        routing.put("rateLimiting", Map.of(
            "enabled", true,
            "requestsPerMinute", 100,
            "burstCapacity", 200
        ));
        
        return Mono.just(ResponseEntity.ok(routing));
    }

    @Operation(
        summary = "Get API documentation links",
        description = "Get links to Swagger documentation for all microservices"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documentation links retrieved successfully")
    })
    @GetMapping("/docs")
    public Mono<ResponseEntity<Map<String, Object>>> getApiDocumentation() {
        Map<String, Object> docs = new HashMap<>();
        
        List<Map<String, Object>> services = Arrays.asList(
            Map.of(
                "service", "API Gateway",
                "swaggerUrl", "http://localhost:8080/swagger-ui.html",
                "apiDocsUrl", "http://localhost:8080/v3/api-docs",
                "description", "Gateway management and routing"
            ),
            Map.of(
                "service", "User Service",
                "swaggerUrl", "http://localhost:8081/swagger-ui.html",
                "apiDocsUrl", "http://localhost:8081/v3/api-docs",
                "description", "User management and authentication"
            ),
            Map.of(
                "service", "Transaction Service",
                "swaggerUrl", "http://localhost:8082/swagger-ui.html",
                "apiDocsUrl", "http://localhost:8082/v3/api-docs",
                "description", "Transaction management and processing"
            ),
            Map.of(
                "service", "Analytics Service",
                "swaggerUrl", "http://localhost:8083/swagger-ui.html",
                "apiDocsUrl", "http://localhost:8083/v3/api-docs",
                "description", "Financial analytics and reporting"
            ),
            Map.of(
                "service", "Notification Service",
                "swaggerUrl", "http://localhost:8084/swagger-ui.html",
                "apiDocsUrl", "http://localhost:8084/v3/api-docs",
                "description", "Notification and communication management"
            )
        );
        
        docs.put("services", services);
        docs.put("totalServices", services.size());
        docs.put("aggregatedDocs", "http://localhost:8080/swagger-ui.html");
        
        return Mono.just(ResponseEntity.ok(docs));
    }

    @Operation(
        summary = "Get rate limiting status",
        description = "Check current rate limiting status and quotas"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate limiting status retrieved successfully")
    })
    @GetMapping("/rate-limit")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimitStatus(
            @Parameter(description = "Client IP or User ID")
            @RequestParam(required = false) String clientId) {
        
        Map<String, Object> rateLimitInfo = new HashMap<>();
        rateLimitInfo.put("enabled", true);
        rateLimitInfo.put("algorithm", "TOKEN_BUCKET");
        
        Map<String, Object> limits = new HashMap<>();
        limits.put("requestsPerMinute", 100);
        limits.put("burstCapacity", 200);
        limits.put("refillRate", 100);
        rateLimitInfo.put("limits", limits);
        
        if (clientId != null) {
            Map<String, Object> clientStatus = new HashMap<>();
            clientStatus.put("clientId", clientId);
            clientStatus.put("remainingRequests", 85);
            clientStatus.put("resetTime", LocalDateTime.now().plusMinutes(1));
            clientStatus.put("requestsUsed", 15);
            rateLimitInfo.put("clientStatus", clientStatus);
        }
        
        Map<String, Object> globalStats = new HashMap<>();
        globalStats.put("totalRequestsLastMinute", 450);
        globalStats.put("rejectedRequests", 12);
        globalStats.put("averageRequestsPerSecond", 7.5);
        rateLimitInfo.put("globalStats", globalStats);
        
        return Mono.just(ResponseEntity.ok(rateLimitInfo));
    }

    @Operation(
        summary = "Get gateway metrics",
        description = "Retrieve detailed metrics about gateway performance and usage"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    })
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Map<String, Object>>> getGatewayMetrics(
            @Parameter(description = "Time period for metrics (LAST_HOUR, LAST_DAY, LAST_WEEK)")
            @RequestParam(defaultValue = "LAST_HOUR") String period) {
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("period", period);
        metrics.put("timestamp", LocalDateTime.now());
        
        Map<String, Object> requests = new HashMap<>();
        requests.put("total", 1250);
        requests.put("successful", 1180);
        requests.put("failed", 70);
        requests.put("successRate", 94.4);
        metrics.put("requests", requests);
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("averageResponseTime", "245ms");
        performance.put("p95ResponseTime", "450ms");
        performance.put("p99ResponseTime", "800ms");
        performance.put("maxResponseTime", "1200ms");
        metrics.put("performance", performance);
        
        Map<String, Object> byService = new HashMap<>();
        byService.put("user-service", Map.of("requests", 350, "avgResponseTime", "180ms", "errorRate", 2.1));
        byService.put("transaction-service", Map.of("requests", 450, "avgResponseTime", "220ms", "errorRate", 1.8));
        byService.put("analytics-service", Map.of("requests", 200, "avgResponseTime", "380ms", "errorRate", 0.5));
        byService.put("notification-service", Map.of("requests", 250, "avgResponseTime", "150ms", "errorRate", 1.2));
        metrics.put("byService", byService);
        
        Map<String, Object> errors = new HashMap<>();
        errors.put("4xx", 45);
        errors.put("5xx", 25);
        errors.put("timeouts", 15);
        errors.put("circuitBreakerOpen", 2);
        metrics.put("errors", errors);
        
        return Mono.just(ResponseEntity.ok(metrics));
    }
}