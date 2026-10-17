package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SsoAuthorizeVO {
    private String authorizeUrl;
    private String state;
}
