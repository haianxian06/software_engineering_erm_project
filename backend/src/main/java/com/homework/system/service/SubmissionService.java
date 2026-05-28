package com.homework.system.service;

import com.homework.system.common.BusinessException;
import com.homework.system.dto.ReviewRequest;
import com.homework.system.dto.StudentSubmissionResponse;
import com.homework.system.dto.SubmissionSaveResult;
import com.homework.system.dto.SubmissionSummary;
import com.homework.system.entity.Assignment;
import com.homework.system.entity.Student;
import com.homework.system.entity.SystemUser;
import com.homework.system.repository.StudentRepository;
import com.homework.system.repository.SubmissionRepository;
import com.homework.system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final AssignmentService assignmentService;
    private final StudentRepository studentRepository;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public SubmissionService(AssignmentService assignmentService,
                             StudentRepository studentRepository,
                             SubmissionRepository submissionRepository,
                             FileStorageService fileStorageService,
                             UserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.studentRepository = studentRepository;
        this.submissionRepository = submissionRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    public void submit(Long assignmentId, String studentNo, String realName, MultipartFile file) throws IOException {
        if (studentNo == null || studentNo.isBlank()) {
            throw new BusinessException("学号不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("提交文件不能为空");
        }

        Assignment assignment = assignmentService.getAssignment(assignmentId);
        if (LocalDateTime.now().isAfter(assignment.deadline())) {
            throw new BusinessException("已超过截止时间，不能提交或修改");
        }
        validateFile(assignment, file);

        Student student = studentRepository.findOrCreateStudent(studentNo, realName, assignment.classId());
        if (!studentRepository.isMemberOfClass(student.id(), assignment.classId())) {
            throw new BusinessException("你不在该作业发布的班级中，不能提交");
        }
        SubmissionSaveResult savedSubmission = submissionRepository.saveSubmission(assignmentId, student.id());
        String effectiveRealName = student.realName() == null || student.realName().isBlank()
                ? realName
                : student.realName();
        String storedName = buildStoredName(
                assignment,
                student.studentNo(),
                effectiveRealName,
                savedSubmission.versionNo(),
                file.getOriginalFilename()
        );
        String storageKey = fileStorageService.save(assignmentId, storedName, file);
        FileProcessingResult processed = fileStorageService.archiveAsPdf(
                assignment,
                storedName,
                Path.of(storageKey),
                file
        );
        submissionRepository.saveFileRecord(
                savedSubmission.submissionId(),
                file.getOriginalFilename(),
                storedName,
                storageKey,
                file.getSize(),
                file.getContentType(),
                processed.processedName(),
                processed.processedStorageKey(),
                processed.processedType()
        );
    }

    public Optional<StudentSubmissionResponse> getCurrentSubmission(Long assignmentId, String studentNo) {
        if (studentNo == null || studentNo.isBlank()) {
            throw new BusinessException("学号不能为空");
        }

        Assignment assignment = assignmentService.getAssignment(assignmentId);
        boolean canModify = !LocalDateTime.now().isAfter(assignment.deadline());
        return submissionRepository.findCurrentSubmission(assignmentId, studentNo)
                .map(summary -> toStudentSubmission(summary, canModify));
    }

    public List<StudentSubmissionResponse> getSubmissionHistory(Long assignmentId, String studentNo) {
        if (studentNo == null || studentNo.isBlank()) {
            throw new BusinessException("学号不能为空");
        }

        Assignment assignment = assignmentService.getAssignment(assignmentId);
        boolean canModify = !LocalDateTime.now().isAfter(assignment.deadline());
        return submissionRepository.findHistoryByAssignmentIdAndStudentNo(assignmentId, studentNo)
                .stream()
                .map(summary -> toStudentSubmission(summary, canModify))
                .toList();
    }

    private StudentSubmissionResponse toStudentSubmission(SubmissionSummary summary, boolean canModify) {
        return new StudentSubmissionResponse(
                summary.submissionId(),
                summary.studentNo(),
                summary.realName(),
                summary.storedName(),
                summary.originalName(),
                summary.fileSize(),
                summary.submitTime(),
                summary.versionNo(),
                summary.status(),
                summary.finalVersion(),
                summary.processedName(),
                summary.processedStorageKey(),
                summary.processedType(),
                canModify,
                summary.reviewStatus(),
                summary.score(),
                summary.reviewComment(),
                summary.reviewedBy(),
                summary.reviewerName(),
                summary.reviewedAt()
        );
    }

    public void reviewSubmission(Long submissionId, ReviewRequest request) {
        if (!submissionRepository.existsById(submissionId)) {
            throw new BusinessException("提交记录不存在");
        }
        SystemUser reviewer = userRepository.findById(request.reviewedBy())
                .orElseThrow(() -> new BusinessException("批阅人不存在"));
        if (!"ADMIN".equals(reviewer.role()) && !userRepository.hasManagePermission(reviewer.id())) {
            throw new BusinessException("只有老师或班级管理员可以批阅");
        }
        submissionRepository.saveReview(
                submissionId,
                request.score(),
                request.comment() == null ? "" : request.comment().trim(),
                reviewer.id()
        );
    }

    private void validateFile(Assignment assignment, MultipartFile file) {
        long maxBytes = assignment.maxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件格式或大小不符，请检查作业要求");
        }

        String ext = getExtension(file.getOriginalFilename());
        String allowed = assignment.fileTypes();
        if (allowed != null && !allowed.isBlank()) {
            Set<String> allowedExtensions = Arrays.stream(allowed.split(","))
                    .map(String::trim)
                    .map(value -> value.startsWith(".") ? value.substring(1) : value)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet());
            if (!allowedExtensions.contains(ext.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("文件格式或大小不符，请检查作业要求");
            }
        }
    }

    private String buildStoredName(Assignment assignment, String studentNo, String realName, Integer versionNo, String originalName) {
        String ext = getExtension(originalName);
        String baseName = applyRenamePattern(assignment, studentNo, realName, versionNo);
        String versionSuffix = shouldAppendVersionSuffix(assignment.renamePattern(), versionNo)
                ? "_v" + versionNo
                : "";
        return cleanName(baseName) + versionSuffix + "." + ext;
    }

    private String applyRenamePattern(Assignment assignment, String studentNo, String realName, Integer versionNo) {
        String pattern = assignment.renamePattern() == null || assignment.renamePattern().isBlank()
                ? "学号_姓名_作业名"
                : assignment.renamePattern();
        String version = String.valueOf(versionNo == null ? 1 : versionNo);
        return pattern
                .replace("{studentNo}", safe(studentNo))
                .replace("{realName}", safe(realName))
                .replace("{assignmentTitle}", safe(assignment.title()))
                .replace("{className}", safe(assignment.className()))
                .replace("{assignmentId}", String.valueOf(assignment.id()))
                .replace("{version}", version)
                .replace("学号", safe(studentNo))
                .replace("姓名", safe(realName))
                .replace("作业名", safe(assignment.title()))
                .replace("班级", safe(assignment.className()))
                .replace("版本", version);
    }

    private boolean shouldAppendVersionSuffix(String renamePattern, Integer versionNo) {
        if (versionNo == null || versionNo <= 1) {
            return false;
        }
        String pattern = renamePattern == null ? "" : renamePattern;
        return !pattern.contains("{version}") && !pattern.contains("版本");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "dat";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String cleanName(String value) {
        return value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
