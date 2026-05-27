package com.homework.system.service;

import com.homework.system.common.BusinessException;
import com.homework.system.dto.ClassMemberRequest;
import com.homework.system.dto.ClassMemberResponse;
import com.homework.system.dto.ClassRequest;
import com.homework.system.dto.MemberRoleRequest;
import com.homework.system.entity.ClassInfo;
import com.homework.system.entity.SystemUser;
import com.homework.system.repository.ClassRepository;
import com.homework.system.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public ClassService(ClassRepository classRepository, UserRepository userRepository) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    public List<ClassInfo> listManageableClasses(Long userId) {
        if (userId == null) {
            throw new BusinessException("缺少当前用户");
        }
        return classRepository.findManageableClasses(userId);
    }

    public ClassInfo createClass(ClassRequest request) {
        userRepository.findById(request.createdBy())
                .orElseThrow(() -> new BusinessException("创建人不存在"));
        Long classId = classRepository.saveClass(
                request.className(),
                request.major(),
                request.courseName(),
                request.grade(),
                request.createdBy()
        );
        classRepository.addMember(classId, request.createdBy(), "TEACHER");
        classRepository.addStudentsByMajor(classId, request.major());
        return classRepository.findManageableClasses(request.createdBy()).stream()
                .filter(item -> item.id().equals(classId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("班级创建失败"));
    }

    public List<ClassMemberResponse> listMembers(Long classId) {
        return classRepository.findMembers(classId);
    }

    public List<ClassMemberResponse> listAvailableStudents(Long classId) {
        return classRepository.findAvailableStudents(classId);
    }

    public void addStudentToClass(Long classId, ClassMemberRequest request) {
        SystemUser student = userRepository.findByStudentNo(request.studentNo())
                .orElseThrow(() -> new BusinessException("学生不存在，请先让学生注册"));
        classRepository.addStudentMember(classId, student.id());
    }

    public void updateMemberRole(Long classId, Long userId, MemberRoleRequest request) {
        SystemUser student = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(student.role())) {
            throw new BusinessException("只能把学生设为管理员");
        }
        String role = request.memberRole().toUpperCase();
        if (!"ADMIN".equals(role) && !"STUDENT".equals(role)) {
            throw new BusinessException("学生只能设置为普通成员或管理员");
        }
        int changed = classRepository.updateMemberRole(classId, userId, role);
        if (changed == 0) {
            throw new BusinessException("该学生不在这个班级中");
        }
    }

    public void removeStudentFromClass(Long classId, Long userId) {
        SystemUser student = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(student.role())) {
            throw new BusinessException("不能移出老师账号");
        }
        int changed = classRepository.removeMember(classId, userId);
        if (changed == 0) {
            throw new BusinessException("该学生不在这个班级中");
        }
    }
}
