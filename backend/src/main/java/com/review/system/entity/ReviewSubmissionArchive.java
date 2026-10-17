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
@TableName(value = "review_submission_archive", autoResultMap = true)
public class ReviewSubmissionArchive {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer reviewRound;
    private Long assignmentId;
    private Long expertId;
    private String expertName;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scores;
    private BigDecimal totalScore;
    private String opinion;
    private String conclusion;
    private LocalDateTime submittedAt;
    private LocalDateTime archivedAt;
}
