package com.review.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class NotificationTemplateUpdateRequest {
    @NotEmpty
    private Map<String, String> templates;
}
