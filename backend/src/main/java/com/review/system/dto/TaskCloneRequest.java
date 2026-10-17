package com.review.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCloneRequest {
    private String name;
    @NotNull
    private LocalDateTime deadline;
}
