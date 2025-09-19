package com.finance.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private UUID userId;
    private String username;
    private String email;
}