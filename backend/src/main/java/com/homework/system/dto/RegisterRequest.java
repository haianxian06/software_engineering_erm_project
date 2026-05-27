package com.homework.system.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "注册身份不能为空")
        String role,
        String studentNo,
        String workNo,
        @NotBlank(message = "姓名不能为空")
        String realName,
        String major
) {
}
