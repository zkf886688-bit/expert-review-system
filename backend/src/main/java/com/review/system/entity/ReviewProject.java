package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_project")
public class ReviewProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String projectName;
    private String applicant;
    private String organization;
    private Long applicantUserId;
    private Integer sortOrder;
    private Integer reviewRound;
    private String rectificationStatus;
    private String rectificationNote;
    private String rectificationReply;
    private LocalDateTime rectificationRequestedAt;
    private LocalDateTime rectificationSubmittedAt;
    private LocalDateTime createdAt;
}
