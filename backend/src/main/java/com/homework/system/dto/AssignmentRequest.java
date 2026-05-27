package com.homework.system.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotBlank(message = "作业标题不能为空")
        String title,
        String description,
        @NotNull(message = "截止时间不能为空")
        @Future(message = "时间设置无效")
        LocalDateTime deadline,
        String fileTypes,
        @Min(value = 1, message = "文件大小限制至少为 1MB")
        Integer maxSizeMb,
        String renamePattern,
        Long classId,
        String createdBy,
        String status
) {
}
