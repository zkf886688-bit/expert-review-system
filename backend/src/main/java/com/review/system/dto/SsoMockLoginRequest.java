package com.review.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SsoMockLoginRequest {
    @NotBlank
    private String username;
}
