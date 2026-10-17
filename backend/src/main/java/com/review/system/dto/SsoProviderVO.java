package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SsoProviderVO {
    private String id;
    private String name;
    private String type;
    /** mock 为 POST；oauth2 需先 GET authorize 再跳转 */
    private String loginMode;
}
