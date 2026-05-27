package com.homework.system.service;

import com.homework.system.common.BusinessException;
import com.homework.system.config.AppProperties;
import com.homework.system.dto.ArchivePackageRecord;
import com.homework.system.dto.ArchivePackageResponse;
import com.homework.system.entity.Assignment;
import com.homework.system.repository.ArchivePackageRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ArchivePackageService {

    private static final DateTimeFormatter ZIP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AssignmentService assignmentService;
    private final FileStorageService fileStorageService;
    private final ArchivePackageRepository archivePackageRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ArchivePackageService(AssignmentService assignmentService,
                                 FileStorageService fileStorageService,
                                 ArchivePackageRepository archivePackageRepository,
                                 AppProperties appProperties) {
        this.assignmentService = assignmentService;
        this.fileStorageService = fileStorageService;
        this.archivePackageRepository = archivePackageRepository;
        this.appProperties = appProperties;
    }

    public ArchivePackageResponse packageAssignment(Long assignmentId) throws IOException {
        Assignment assignment = assignmentService.getAssignment(assignmentId);
        Path sourceDir = fileStorageService.resolveAssignmentArchiveDir(assignment);
        if (!Files.isDirectory(sourceDir)) {
            throw new BusinessException("归档目录暂无文件，不能打包");
        }

        List<Path> archiveFiles;
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            archiveFiles = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        if (archiveFiles.isEmpty()) {
            throw new BusinessException("归档目录暂无文件，不能打包");
        }

        Path packageDir = resolvePackageDir();
        Files.createDirectories(packageDir);
        String zipName = cleanName(assignment.className()) + "_" + cleanName(assignment.title())
                + "_" + ZIP_TIME_FORMAT.format(LocalDateTime.now()) + ".zip";
        Path zipPath = packageDir.resolve(zipName);
        writeZip(sourceDir, archiveFiles, zipPath);

        String token = generateToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resolveTtlMinutes());
        long packageSize = Files.size(zipPath);
        archivePackageRepository.save(
                assignment.id(),
                zipName,
                zipPath.toString().replace("\\", "/"),
                packageSize,
                tokenHash,
                expiresAt
        );

        return new ArchivePackageResponse(
                assignment.id(),
                zipName,
                packageSize,
                "/api/packages/download/" + token,
                expiresAt
        );
    }

    public ArchivePackageRecord getDownloadPackage(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("下载链接无效");
        }
        Optional<ArchivePackageRecord> packageRecord = archivePackageRepository.findByTokenHash(hashToken(token));
        ArchivePackageRecord record = packageRecord.orElseThrow(() -> new BusinessException("下载链接无效"));
        if (LocalDateTime.now().isAfter(record.expiresAt())) {
            throw new BusinessException("下载链接已过期");
        }
        if (!Files.isRegularFile(Path.of(record.zipPath()))) {
            throw new BusinessException("打包文件不存在");
        }
        return record;
    }

    private void writeZip(Path sourceDir, List<Path> archiveFiles, Path zipPath) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path file : archiveFiles) {
                String entryName = sourceDir.relativize(file).toString().replace("\\", "/");
                zip.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private Path resolvePackageDir() {
        if (appProperties.getPackageDir() != null && !appProperties.getPackageDir().isBlank()) {
            return Path.of(appProperties.getPackageDir());
        }
        return Path.of(appProperties.getArchiveDir()).getParent().resolve("packages");
    }

    private int resolveTtlMinutes() {
        return appProperties.getPackageLinkTtlMinutes() == null
                ? 30
                : Math.max(appProperties.getPackageLinkTtlMinutes(), 1);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 not available", error);
        }
    }

    private String cleanName(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
