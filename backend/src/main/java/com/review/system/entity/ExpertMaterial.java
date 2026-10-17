package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("expert_material")
public class ExpertMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expertId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String materialType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
