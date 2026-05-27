package com.homework.system.entity;

public record Student(
        Long id,
        Long classId,
        String studentNo,
        String realName
) {
}
