package com.homework.system.controller;

import com.homework.system.dto.ArchivePackageRecord;
import com.homework.system.dto.ArchivePackageResponse;
import com.homework.system.service.ArchivePackageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/packages")
public class ArchivePackageController {

    private final ArchivePackageService archivePackageService;

    public ArchivePackageController(ArchivePackageService archivePackageService) {
        this.archivePackageService = archivePackageService;
    }

    @PostMapping("/assignments/{assignmentId}")
    public ArchivePackageResponse packageAssignment(@PathVariable Long assignmentId) throws IOException {
        return archivePackageService.packageAssignment(assignmentId);
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        ArchivePackageRecord record = archivePackageService.getDownloadPackage(token);
        Resource resource = new FileSystemResource(Path.of(record.zipPath()));
        String encodedName = URLEncoder.encode(record.zipName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentLength(record.packageSize())
                .body(resource);
    }
}
