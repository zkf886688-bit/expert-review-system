package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "review_submission", autoResultMap = true)
public class ReviewSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long taskId;
    private Long projectId;
    private Long expertId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scores;
    private BigDecimal totalScore;
    private String opinion;
    private String conclusion;
    private String attachmentPath;
    private String attachmentName;
    private String status;
    private Integer version;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
