package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplatePreviewVO {
    private String title;
    private String content;
}
