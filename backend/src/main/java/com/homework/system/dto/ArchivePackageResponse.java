package com.homework.system.dto;

import java.time.LocalDateTime;

public record ArchivePackageResponse(
        Long assignmentId,
        String zipName,
        Long packageSize,
        String downloadUrl,
        LocalDateTime expiresAt
) {
}
