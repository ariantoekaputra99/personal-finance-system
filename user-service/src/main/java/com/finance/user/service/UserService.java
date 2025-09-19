package com.finance.user.service;

import com.finance.user.dto.*;
import com.finance.user.entity.User;
import com.finance.user.event.UserEvent;
import com.finance.user.repository.UserRepository;
import com.finance.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                      KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Autowired(required = false)
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public UserResponse createUser(UserRegistrationRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to register with existing email: {}", request.getEmail());
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Attempt to register with existing username: {}", request.getUsername());
            throw new IllegalArgumentException("User with username '" + request.getUsername() + "' already exists");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Publish user creation event
        publishUserEvent(savedUser, "USER_CREATED");
        
        return mapToUserResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail());
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(UUID id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    public UserResponse getUserByIdNoCache(UUID id) {
        log.info("Fetching user by id (no cache): {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        log.info("Found user: {} with email: {}", user.getUsername(), user.getEmail());
        return mapToUserResponse(user);
    }

    @Cacheable(value = "users", key = "#email")
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    @Cacheable(value = "users", key = "#username")
    public UserResponse getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        log.info("Updating user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        
        User savedUser = userRepository.save(user);
        
        // Publish user update event
        publishUserEvent(savedUser, "USER_UPDATED");
        
        return mapToUserResponse(savedUser);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        // Publish user deletion event
        publishUserEvent(user, "USER_DELETED");
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        log.info("Changing password for user id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Publish password change event
        publishUserEvent(user, "PASSWORD_CHANGED");
    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getActiveUsers() {
        log.info("Fetching active users");
        return userRepository.findByIsActiveTrue().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserStatisticsResponse getUserStatistics(UUID id) {
        log.info("Fetching user statistics for id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return UserStatisticsResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .transactionCount(0L) // This would be calculated from transaction service
                .totalIncome(java.math.BigDecimal.ZERO)
                .totalExpenses(java.math.BigDecimal.ZERO)
                .netWorth(java.math.BigDecimal.ZERO)
                .accountCount(0L)
                .activityRank(0L)
                .build();
    }

    public List<UserStatisticsResponse> getAllUserStatistics() {
        log.info("Fetching statistics for all users");
        return userRepository.findByIsActiveTrue().stream()
                .map(user -> UserStatisticsResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .createdAt(user.getCreatedAt())
                        .transactionCount(0L)
                        .totalIncome(java.math.BigDecimal.ZERO)
                        .totalExpenses(java.math.BigDecimal.ZERO)
                        .netWorth(java.math.BigDecimal.ZERO)
                        .accountCount(0L)
                        .activityRank(0L)
                        .build())
                .collect(Collectors.toList());
    }

    public List<UserResponse> searchUsers(String query) {
        log.info("Searching users with query: {}", query);
        // Simple search implementation since the complex repository method doesn't exist
        return userRepository.findAll().stream()
                .filter(user -> 
                    user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                    user.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(query.toLowerCase()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersWithRecentActivity(int days) {
        log.info("Fetching users with recent activity in last {} days", days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        // Fallback to users created in the last N days since transactions table may not exist
        return userRepository.findAll().stream()
                .filter(user -> user.getIsActive() && user.getCreatedAt().isAfter(since))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void activateUser(UUID id) {
        log.info("Activating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        publishUserEvent(user, "USER_ACTIVATED");
    }

    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        publishUserEvent(user, "USER_DEACTIVATED");
    }

    private void publishUserEvent(User user, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("userId", user.getId());
        event.put("username", user.getUsername());
        event.put("email", user.getEmail());
        event.put("fullName", user.getFullName());
        event.put("isActive", user.getIsActive());
        event.put("timestamp", LocalDateTime.now());
        
        kafkaTemplate.send("user-events", user.getId().toString(), event);
        log.info("Published user event: {} for user: {}", eventType, user.getId());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .build();
    }
}