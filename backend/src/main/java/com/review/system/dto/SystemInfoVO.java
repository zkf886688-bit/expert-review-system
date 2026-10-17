package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemInfoVO {
    private String appName;
    private String version;
    private boolean mailEnabled;
    private boolean smsEnabled;
    private String mailMode;
    private String smsProvider;
    private boolean mailConfigured;
    private boolean smsConfigured;
    private long userCount;
    private String healthStatus;
}
