package com.homework.system.dto;

public record LoginResponse(
        Long id,
        String username,
        String realName,
        String studentNo,
        String workNo,
        String major,
        String role
) {
}
