package com.homework.system.service;

import com.homework.system.common.BusinessException;
import com.homework.system.dto.LoginRequest;
import com.homework.system.dto.LoginResponse;
import com.homework.system.dto.RegisterRequest;
import com.homework.system.entity.SystemUser;
import com.homework.system.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse register(RegisterRequest request) {
        if ("STUDENT".equalsIgnoreCase(request.role())) {
            if (request.studentNo() == null || request.studentNo().isBlank()) {
                throw new BusinessException("学生注册需要填写学号");
            }
            if (request.major() == null || request.major().isBlank()) {
                throw new BusinessException("学生注册需要填写专业");
            }
            if (userRepository.findByStudentNo(request.studentNo()).isPresent()) {
                throw new BusinessException("该学号已注册");
            }
            Long id = userRepository.saveStudent(request.studentNo(), request.realName(), request.major());
            return toResponse(userRepository.findById(id).orElseThrow());
        }

        if (request.workNo() == null || request.workNo().isBlank()) {
            throw new BusinessException("老师注册需要填写学工号");
        }
        if (userRepository.findByWorkNoOrUsername(request.workNo()).isPresent()) {
            throw new BusinessException("该学工号已注册");
        }
        Long id = userRepository.saveTeacher(request.workNo(), request.realName());
        return toResponse(userRepository.findById(id).orElseThrow());
    }

    public LoginResponse login(LoginRequest request) {
        SystemUser user;
        if ("STUDENT".equalsIgnoreCase(request.role())) {
            user = userRepository.findByStudentNo(request.studentNo())
                    .orElseThrow(() -> new BusinessException("学生不存在，请先注册"));
        } else {
            String value = request.workNo() == null || request.workNo().isBlank()
                    ? request.username()
                    : request.workNo();
            user = userRepository.findByWorkNoOrUsername(value)
                    .orElseThrow(() -> new BusinessException("老师不存在，请先注册"));
        }
        return toResponse(user);
    }

    private LoginResponse toResponse(SystemUser user) {
        String viewRole = "ADMIN".equals(user.role()) || userRepository.hasManagePermission(user.id())
                ? "ADMIN"
                : "STUDENT";
        return new LoginResponse(
                user.id(),
                user.username(),
                user.realName(),
                user.studentNo(),
                user.workNo(),
                user.major(),
                viewRole
        );
    }
}
