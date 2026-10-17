package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RemindResultVO {
    private int remindedCount;
}
