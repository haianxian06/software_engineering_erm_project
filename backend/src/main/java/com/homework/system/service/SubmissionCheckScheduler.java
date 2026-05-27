package com.homework.system.service;

import com.homework.system.entity.Assignment;
import com.homework.system.repository.AssignmentRepository;
import com.homework.system.repository.SubmissionCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SubmissionCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubmissionCheckScheduler.class);

    private final AssignmentRepository assignmentRepository;
    private final SubmissionCheckRepository submissionCheckRepository;

    public SubmissionCheckScheduler(AssignmentRepository assignmentRepository,
                                    SubmissionCheckRepository submissionCheckRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionCheckRepository = submissionCheckRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshAllPublishedAssignments();
    }

    @Scheduled(fixedDelayString = "${app.submission-check-interval-ms:30000}", initialDelayString = "5000")
    public void refreshAllPublishedAssignments() {
        try {
            for (Assignment assignment : assignmentRepository.findPublished()) {
                submissionCheckRepository.refreshAssignment(assignment.id(), assignment.classId());
            }
        } catch (Exception error) {
            log.warn("定时检查提交状态失败: {}", error.getMessage());
        }
    }
}
