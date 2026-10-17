package com.review.system.dto;

import lombok.Data;

@Data
public class ExpertQuery {
    private String name;
    private String organization;
    private String field;
    private String tag;
    private String region;
    private String title;
    private String status;
    private int page = 1;
    private int size = 10;
}
