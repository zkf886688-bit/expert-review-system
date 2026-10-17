package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RectificationRequestDto {
    @NotBlank(message = "请填写整改要求")
    private String note;
}
