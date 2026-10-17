package com.review.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RectificationVO {
    private String status;
    private Integer reviewRound;
    private String note;
    private String reply;
    private LocalDateTime requestedAt;
    private LocalDateTime submittedAt;
    private boolean canSubmit;
    private boolean canCancel;
    private boolean canStartReReview;
    private List<MaterialItem> materials;

    @Data
    @Builder
    public static class MaterialItem {
        private Long id;
        private String fileName;
        private Long fileSize;
        private LocalDateTime createdAt;
    }
}
