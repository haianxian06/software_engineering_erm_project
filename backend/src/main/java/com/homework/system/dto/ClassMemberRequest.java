package com.homework.system.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassMemberRequest(
        @NotBlank(message = "学号不能为空")
        String studentNo
) {
}
