package com.homework.system.controller;

import com.homework.system.dto.ReviewRequest;
import com.homework.system.dto.StudentSubmissionResponse;
import com.homework.system.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/current")
    public ResponseEntity<StudentSubmissionResponse> getCurrentSubmission(@RequestParam Long assignmentId,
                                                                          @RequestParam String studentNo) {
        Optional<StudentSubmissionResponse> submission = submissionService.getCurrentSubmission(assignmentId, studentNo);
        return submission.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public List<StudentSubmissionResponse> getSubmissionHistory(@RequestParam Long assignmentId,
                                                                @RequestParam String studentNo) {
        return submissionService.getSubmissionHistory(assignmentId, studentNo);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@RequestParam Long assignmentId,
                                                      @RequestParam String studentNo,
                                                      @RequestParam String realName,
                                                      @RequestParam MultipartFile file) throws IOException {
        submissionService.submit(assignmentId, studentNo, realName, file);
        return ResponseEntity.ok(Map.of("message", "提交成功"));
    }

    @PutMapping("/{submissionId}/review")
    public ResponseEntity<Map<String, String>> review(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ReviewRequest request) {
        submissionService.reviewSubmission(submissionId, request);
        return ResponseEntity.ok(Map.of("message", "批阅已保存"));
    }
}
