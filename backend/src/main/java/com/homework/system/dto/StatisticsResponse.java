package com.homework.system.dto;

import com.homework.system.entity.Student;

import java.time.LocalDateTime;
import java.util.List;

public record StatisticsResponse(
        Long assignmentId,
        String assignmentTitle,
        String className,
        LocalDateTime deadline,
        Boolean channelClosed,
        Integer totalStudents,
        Integer submittedCount,
        Integer submitRate,
        List<SubmissionSummary> submissions,
        List<Student> missingStudents,
        LocalDateTime lastCheckedAt
) {
}
