package com.review.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class RemindRequest {
    /** 为空则催办全部待确认/未提交专家 */
    private List<Long> assignmentIds;
}
