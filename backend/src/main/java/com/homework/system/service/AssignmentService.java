package com.homework.system.service;

import com.homework.system.common.BusinessException;
import com.homework.system.dto.AssignmentRequest;
import com.homework.system.dto.SubmissionCheckSnapshot;
import com.homework.system.dto.StatisticsResponse;
import com.homework.system.dto.SubmissionSummary;
import com.homework.system.entity.Assignment;
import com.homework.system.entity.Student;
import com.homework.system.repository.AssignmentRepository;
import com.homework.system.repository.StudentRepository;
import com.homework.system.repository.SubmissionCheckRepository;
import com.homework.system.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionCheckRepository submissionCheckRepository;
    private final FileStorageService fileStorageService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             StudentRepository studentRepository,
                             SubmissionRepository submissionRepository,
                             SubmissionCheckRepository submissionCheckRepository,
                             FileStorageService fileStorageService) {
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.submissionRepository = submissionRepository;
        this.submissionCheckRepository = submissionCheckRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Assignment> listAssignments() {
        return assignmentRepository.findAll();
    }

    public List<Assignment> listAssignments(Long userId, String role) {
        if (userId == null) {
            return listAssignments();
        }
        if ("STUDENT".equalsIgnoreCase(role)) {
            return assignmentRepository.findForStudent(userId);
        }
        return assignmentRepository.findForManager(userId);
    }

    public Assignment getAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("作业不存在"));
    }

    public Assignment createAssignment(AssignmentRequest request) {
        Assignment assignment = new Assignment(
                null,
                request.title(),
                request.description(),
                request.deadline(),
                request.fileTypes() == null || request.fileTypes().isBlank() ? "zip,docx,pdf" : request.fileTypes(),
                request.maxSizeMb() == null ? 100 : request.maxSizeMb(),
                normalizeRenamePattern(request.renamePattern()),
                request.classId() == null ? 1L : request.classId(),
                null,
                request.createdBy() == null || request.createdBy().isBlank() ? "admin" : request.createdBy(),
                null,
                normalizeStatus(request.status()),
                LocalDateTime.now()
        );
        Long id = assignmentRepository.save(assignment);
        return getAssignment(id);
    }

    public Assignment updateAssignment(Long id, AssignmentRequest request) {
        Assignment existing = getAssignment(id);
        Assignment assignment = new Assignment(
                id,
                request.title(),
                request.description(),
                request.deadline(),
                request.fileTypes() == null || request.fileTypes().isBlank() ? "zip,docx,pdf" : request.fileTypes(),
                request.maxSizeMb() == null ? 100 : request.maxSizeMb(),
                normalizeRenamePattern(request.renamePattern()),
                request.classId() == null ? existing.classId() : request.classId(),
                existing.className(),
                existing.createdBy(),
                existing.teacherName(),
                normalizeStatus(request.status()),
                existing.createdAt()
        );
        assignmentRepository.update(assignment);
        return getAssignment(id);
    }

    public StatisticsResponse getStatistics(Long assignmentId) {
        Assignment assignment = getAssignment(assignmentId);
        List<SubmissionSummary> submissions = submissionRepository.findSummariesByAssignmentId(assignmentId);
        List<Student> missingStudents = studentRepository.findMissingStudents(assignmentId, assignment.classId());
        int total = studentRepository.countByClassId(assignment.classId());
        int submitted = submissions.size();
        int rate = total == 0 ? 0 : Math.round(submitted * 100f / total);
        SubmissionCheckSnapshot snapshot = submissionCheckRepository.findSnapshot(assignmentId);
        return new StatisticsResponse(
                assignment.id(),
                assignment.title(),
                assignment.className(),
                assignment.deadline(),
                LocalDateTime.now().isAfter(assignment.deadline()),
                total,
                submitted,
                rate,
                submissions,
                missingStudents,
                snapshot.checkedAt()
        );
    }

    @Transactional
    public void deleteAssignment(Long id) {
        Assignment assignment = getAssignment(id);
        List<String> artifactPaths = assignmentRepository.findArtifactPaths(id);
        int deleted = assignmentRepository.deleteById(id);
        if (deleted == 0) {
            throw new BusinessException("作业不存在或已被删除");
        }
        try {
            fileStorageService.deleteAssignmentArtifacts(assignment, artifactPaths);
        } catch (IOException e) {
            throw new BusinessException("作业记录已删除，但文件清理失败：" + e.getMessage());
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PUBLISHED";
        }
        String value = status.trim().toUpperCase();
        if (!"PUBLISHED".equals(value) && !"DRAFT".equals(value)) {
            throw new BusinessException("作业状态不合法");
        }
        return value;
    }

    private String normalizeRenamePattern(String renamePattern) {
        if (renamePattern == null || renamePattern.isBlank()) {
            return "学号_姓名_作业名";
        }
        return renamePattern.trim();
    }
}
