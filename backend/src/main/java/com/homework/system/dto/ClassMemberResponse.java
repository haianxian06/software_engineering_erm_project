package com.homework.system.dto;

public record ClassMemberResponse(
        Long userId,
        String studentNo,
        String workNo,
        String realName,
        String major,
        String systemRole,
        String memberRole
) {
}
