package com.homework.system.entity;

import java.time.LocalDateTime;

public record Assignment(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        String fileTypes,
        Integer maxSizeMb,
        String renamePattern,
        Long classId,
        String className,
        String createdBy,
        String teacherName,
        String status,
        LocalDateTime createdAt
) {
}
