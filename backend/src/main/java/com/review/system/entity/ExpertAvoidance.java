package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("expert_avoidance")
public class ExpertAvoidance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expertId;
    private String avoidType;
    private String avoidValue;
    private String reason;
    private LocalDateTime createdAt;
}
