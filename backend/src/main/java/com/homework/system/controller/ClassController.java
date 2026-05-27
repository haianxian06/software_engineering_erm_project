package com.homework.system.controller;

import com.homework.system.dto.ClassMemberRequest;
import com.homework.system.dto.ClassMemberResponse;
import com.homework.system.dto.ClassRequest;
import com.homework.system.dto.MemberRoleRequest;
import com.homework.system.entity.ClassInfo;
import com.homework.system.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public List<ClassInfo> listManageableClasses(@RequestParam Long userId) {
        return classService.listManageableClasses(userId);
    }

    @PostMapping
    public ClassInfo createClass(@Valid @RequestBody ClassRequest request) {
        return classService.createClass(request);
    }

    @GetMapping("/{id}/members")
    public List<ClassMemberResponse> listMembers(@PathVariable Long id) {
        return classService.listMembers(id);
    }

    @GetMapping("/{id}/available-students")
    public List<ClassMemberResponse> listAvailableStudents(@PathVariable Long id) {
        return classService.listAvailableStudents(id);
    }

    @PostMapping("/{id}/members")
    public void addStudentToClass(@PathVariable Long id,
                                  @Valid @RequestBody ClassMemberRequest request) {
        classService.addStudentToClass(id, request);
    }

    @PutMapping("/{classId}/members/{userId}/role")
    public void updateMemberRole(@PathVariable Long classId,
                                 @PathVariable Long userId,
                                 @Valid @RequestBody MemberRoleRequest request) {
        classService.updateMemberRole(classId, userId, request);
    }

    @DeleteMapping("/{classId}/members/{userId}")
    public void removeStudentFromClass(@PathVariable Long classId,
                                       @PathVariable Long userId) {
        classService.removeStudentFromClass(classId, userId);
    }
}
