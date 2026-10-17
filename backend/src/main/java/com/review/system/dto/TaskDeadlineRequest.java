package com.review.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskDeadlineRequest {
    @NotNull
    private LocalDateTime deadline;
}
