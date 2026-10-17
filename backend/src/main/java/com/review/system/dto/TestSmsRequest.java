package com.review.system.dto;

import lombok.Data;

@Data
public class TestSmsRequest {
    private String phone;
    private String content;
}
