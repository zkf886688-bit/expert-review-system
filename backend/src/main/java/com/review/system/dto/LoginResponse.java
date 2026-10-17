package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long expertId;
    private List<String> roles;
}
