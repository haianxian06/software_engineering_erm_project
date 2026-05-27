package com.homework.system.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberRoleRequest(
        @NotBlank(message = "成员角色不能为空")
        String memberRole
) {
}
