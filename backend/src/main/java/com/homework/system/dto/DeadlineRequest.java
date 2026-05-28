package com.homework.system.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DeadlineRequest(
        @NotNull(message = "截止时间不能为空")
        @Future(message = "时间设置无效")
        LocalDateTime deadline
) {
}
