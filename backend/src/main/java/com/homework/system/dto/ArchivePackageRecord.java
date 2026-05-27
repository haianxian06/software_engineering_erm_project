package com.homework.system.dto;

import java.time.LocalDateTime;

public record ArchivePackageRecord(
        Long id,
        Long assignmentId,
        String zipName,
        String zipPath,
        Long packageSize,
        LocalDateTime expiresAt
) {
}
