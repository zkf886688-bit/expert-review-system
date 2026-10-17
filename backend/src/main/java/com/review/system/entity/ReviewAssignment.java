package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_assignment")
public class ReviewAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long projectId;
    private Long expertId;
    private String status;
    private String rejectReason;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime submittedAt;
}
