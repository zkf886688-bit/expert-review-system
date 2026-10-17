package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationTemplateVO {
    private Map<String, String> templates;
    private Map<String, String> defaults;
    private Map<String, String> placeholders;
    private Map<String, String> sampleVars;
}
