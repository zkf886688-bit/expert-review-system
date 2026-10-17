package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private Long expertId;
    private List<String> roles;
    private String dataDomain;
    private LocalDateTime createdAt;
}
