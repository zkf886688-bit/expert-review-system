package com.review.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OpinionTemplateUpdateRequest {
    @NotNull
    private List<String> templates;
}
