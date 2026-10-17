package com.review.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignRequest {
    @NotNull
    private Long projectId;
    @NotEmpty
    private List<Long> expertIds;
}
