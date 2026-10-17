package com.review.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaskFormConfigUpdateRequest {
    @NotEmpty
    private List<Map<String, Object>> formConfig;
}
