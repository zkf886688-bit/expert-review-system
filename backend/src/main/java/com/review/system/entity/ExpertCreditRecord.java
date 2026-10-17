package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("expert_credit_record")
public class ExpertCreditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expertId;
    private Long assignmentId;
    private Long submissionId;
    private String recordType;
    private BigDecimal score;
    private String comment;
    private Long createdBy;
    private LocalDateTime createdAt;
}
