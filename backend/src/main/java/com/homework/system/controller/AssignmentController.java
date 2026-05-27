package com.homework.system.controller;

import com.homework.system.dto.AssignmentRequest;
import com.homework.system.dto.StatisticsResponse;
import com.homework.system.entity.Assignment;
import com.homework.system.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<Assignment> listAssignments(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String role) {
        return assignmentService.listAssignments(userId, role);
    }

    @GetMapping("/{id}")
    public Assignment getAssignment(@PathVariable Long id) {
        return assignmentService.getAssignment(id);
    }

    @PostMapping
    public Assignment createAssignment(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.createAssignment(request);
    }

    @PutMapping("/{id}")
    public Assignment updateAssignment(@PathVariable Long id,
                                       @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.updateAssignment(id, request);
    }

    @GetMapping("/{id}/statistics")
    public StatisticsResponse getStatistics(@PathVariable Long id) {
        return assignmentService.getStatistics(id);
    }
}
