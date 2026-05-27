package com.homework.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClassRequest(
        @NotBlank(message = "班级名称不能为空")
        String className,
        @NotBlank(message = "专业不能为空")
        String major,
        @NotBlank(message = "课程名称不能为空")
        String courseName,
        String grade,
        @NotNull(message = "创建人不能为空")
        Long createdBy
) {
}
