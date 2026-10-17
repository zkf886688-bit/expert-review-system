package com.review.system.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String realName;
    private String email;
    private String phone;
}
