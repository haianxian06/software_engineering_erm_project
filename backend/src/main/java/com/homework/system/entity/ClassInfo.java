package com.homework.system.entity;

import java.time.LocalDateTime;

public record ClassInfo(
        Long id,
        String className,
        String major,
        String courseName,
        String grade,
        Long createdBy,
        LocalDateTime createdAt,
        Integer memberCount
) {
}
