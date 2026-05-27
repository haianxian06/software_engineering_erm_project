package com.homework.system.entity;

import java.time.LocalDateTime;

public record SystemUser(
        Long id,
        String username,
        String realName,
        String studentNo,
        String workNo,
        String major,
        String role,
        Long classId,
        LocalDateTime createdAt
) {
}
