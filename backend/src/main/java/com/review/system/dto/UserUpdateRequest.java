package com.review.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private List<String> roles;
    private String newPassword;
    private String dataDomain;
}
