package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "review_task", autoResultMap = true)
public class ReviewTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String reviewType;
    private String batchName;
    private String dataDomain;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private Integer minReviewers;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> formConfig;
    private Boolean blindReview;
    private Boolean avoidSameOrg;
    private Boolean avoidDuplicateExpert;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
