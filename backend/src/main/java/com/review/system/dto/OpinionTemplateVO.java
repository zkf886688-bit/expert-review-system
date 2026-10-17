package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpinionTemplateVO {
    private List<String> templates;
}
