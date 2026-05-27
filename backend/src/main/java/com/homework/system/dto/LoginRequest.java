package com.homework.system.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "登录身份不能为空")
        String role,
        String studentNo,
        String workNo,
        String username,
        String realName
) {
}
