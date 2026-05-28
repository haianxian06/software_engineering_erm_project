package com.homework.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReviewRequest(
        @NotNull(message = "分数不能为空")
        @DecimalMin(value = "0", message = "分数不能小于0")
        @DecimalMax(value = "100", message = "分数不能大于100")
        BigDecimal score,
        String comment,
        @NotNull(message = "批阅人不能为空")
        Long reviewedBy
) {
}
