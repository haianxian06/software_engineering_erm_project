package com.homework.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentSubmissionResponse(
        Long submissionId,
        String studentNo,
        String realName,
        String storedName,
        String originalName,
        Long fileSize,
        LocalDateTime submitTime,
        Integer versionNo,
        String status,
        Boolean finalVersion,
        String processedName,
        String processedStorageKey,
        String processedType,
        Boolean canModify,
        String reviewStatus,
        BigDecimal score,
        String reviewComment,
        Long reviewedBy,
        String reviewerName,
        LocalDateTime reviewedAt
) {
}
