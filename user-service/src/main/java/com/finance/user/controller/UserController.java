package com.finance.user.controller;

import com.finance.user.dto.*;
import com.finance.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User Controller with comprehensive REST API
 * Demonstrates Spring MVC integration with all services
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user registration, authentication, and profile management")
public class UserController {

    private final UserService userService;

    /**
     * User login
     */
    @Operation(
        summary = "User login",
        description = "Authenticate user and return JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "User login credentials", required = true)
            @Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Register new user
     */
    @Operation(
        summary = "Register new user",
        description = "Create a new user account with username, email, and password"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Parameter(description = "User registration details", required = true)
            @Valid @RequestBody UserRegistrationRequest request) {
        try {
            log.info("Received user registration request for username: {}", request.getUsername());
            UserResponse response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Registration failed for username: {}, error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during registration for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID
     */
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve user information by user ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {
        try {
            UserResponse user = userService.getUserByIdNoCache(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            log.error("User not found for userId: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user profile
     */
    @Operation(
        summary = "Get user profile",
        description = "Retrieve detailed user profile information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getUserProfile(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {
        log.info("Getting profile for userId: {}", userId);
        try {
            UserResponse user = userService.getUserByIdNoCache(userId);
            log.info("Profile found for userId: {}, username: {}", userId, user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Profile not found for userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update user profile
     */
    @Operation(
        summary = "Update user profile",
        description = "Update user profile information such as name and email"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Updated user information", required = true)
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active users
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        List<UserResponse> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get users with recent activity
     */
    @GetMapping("/recent-activity")
    public ResponseEntity<List<UserResponse>> getUsersWithRecentActivity(
            @RequestParam(defaultValue = "30") int days) {
        List<UserResponse> users = userService.getUsersWithRecentActivity(days);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<UserStatisticsResponse>> getUserStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<UserStatisticsResponse> statistics = userService.getAllUserStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Analyze user spending patterns
     */
    @GetMapping("/spending-patterns")
    public ResponseEntity<Map<String, Object>> analyzeSpendingPatterns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(defaultValue = "0.0") Double minAmount,
            @RequestParam(defaultValue = "1000000.0") Double maxAmount) {
        try {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("totalUsers", userService.getActiveUsers().size());
            analysis.put("dateRange", startDate != null ? startDate.toString() : "all-time");
            analysis.put("minAmount", minAmount);
            analysis.put("maxAmount", maxAmount);
            analysis.put("patterns", new ArrayList<>());
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error analyzing spending patterns", e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * Get cohort analysis
     */
    @GetMapping("/cohort-analysis")
    public ResponseEntity<List<Map<String, Object>>> getCohortAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cohortStartDate) {
        try {
            List<Map<String, Object>> analysis = new ArrayList<>();
            Map<String, Object> cohort = new HashMap<>();
            cohort.put("cohortDate", cohortStartDate != null ? cohortStartDate.toString() : LocalDateTime.now().toString());
            cohort.put("userCount", userService.getActiveUsers().size());
            cohort.put("retentionRate", 0.0);
            analysis.add(cohort);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error getting cohort analysis", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Deactivate user
     */
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate a user account (soft delete)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}