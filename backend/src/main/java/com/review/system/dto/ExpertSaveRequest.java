package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ExpertSaveRequest {
    @NotBlank
    private String name;
    private String gender;
    private String organization;
    private String title;
    private String education;
    private String phone;
    private String email;
    private String idCard;
    private String bankAccount;
    private String region;
    private String orgType;
    private List<String> fields;
    private List<String> tags;
    private String status;
    private String remark;
}
