package com.homework.system.dto;

import java.time.LocalDateTime;

public record SubmissionCheckSnapshot(
        Integer totalStudents,
        Integer submittedCount,
        Integer missingCount,
        Integer submitRate,
        LocalDateTime checkedAt
) {
}
