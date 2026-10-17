package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
