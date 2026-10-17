package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_rectification_material")
public class ProjectRectificationMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
