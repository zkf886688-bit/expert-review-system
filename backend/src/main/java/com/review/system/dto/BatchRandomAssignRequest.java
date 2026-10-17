package com.review.system.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class BatchRandomAssignRequest {
    @Min(1)
    private int count = 3;
    private String field;
    private boolean excludeSameOrg = true;
    /** 已分配足够专家的项目跳过 */
    private boolean skipAssigned = true;
}
