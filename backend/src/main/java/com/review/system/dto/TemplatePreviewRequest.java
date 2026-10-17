package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TemplatePreviewRequest {
    @NotBlank
    private String templateKey;
    private Map<String, String> vars;
}
