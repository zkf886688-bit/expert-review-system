package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResultVO {
    private int successCount;
    private List<String> errors;
}
