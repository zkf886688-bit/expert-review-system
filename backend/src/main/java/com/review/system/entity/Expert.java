package com.review.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "expert", autoResultMap = true)
public class Expert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String gender;
    private String organization;
    private String title;
    private String education;
    private String phone;
    private String email;
    private String idCard;
    private String bankAccount;
    private String region;
    private String orgType;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fields;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    private String status;
    private Integer reviewCount;
    private BigDecimal onTimeRate;
    private BigDecimal avgQualityScore;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
