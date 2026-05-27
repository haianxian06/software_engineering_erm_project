package com.homework.system.service;

public record FileProcessingResult(
        String processedName,
        String processedStorageKey,
        String processedType
) {
}
