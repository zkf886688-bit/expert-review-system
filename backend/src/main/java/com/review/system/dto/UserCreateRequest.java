package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateRequest {
    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 6, message = "密码至少 6 位")
    private String password;
    private String realName;
    private String email;
    private String phone;
    private List<String> roles;
    private String dataDomain;
}
