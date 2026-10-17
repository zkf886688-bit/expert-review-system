package com.review.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RandomAssignRequest {
    @NotNull
    private Long projectId;
    @Min(1)
    private int count = 3;
    private String field;
    private boolean excludeSameOrg = true;
}
