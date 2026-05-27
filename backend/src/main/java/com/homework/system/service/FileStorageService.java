package com.homework.system.service;

import com.homework.system.config.AppProperties;
import com.homework.system.entity.Assignment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class FileStorageService {

    private final AppProperties appProperties;

    public FileStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String save(Long assignmentId, String storedName, MultipartFile file) throws IOException {
        Path targetDir = Path.of(appProperties.getUploadDir(), String.valueOf(assignmentId));
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(storedName);
        file.transferTo(target);
        return target.toString().replace("\\", "/");
    }

    public FileProcessingResult archiveAsPdf(Assignment assignment,
                                             String storedName,
                                             Path originalPath,
                                             MultipartFile file) throws IOException {
        Path targetDir = resolveAssignmentArchiveDir(assignment);
        Files.createDirectories(targetDir);

        String processedName = removeExtension(storedName) + ".pdf";
        Path processedPath = targetDir.resolve(processedName);
        String ext = getExtension(storedName);
        String processedType;

        if ("pdf".equals(ext)) {
            Files.copy(originalPath, processedPath, StandardCopyOption.REPLACE_EXISTING);
            processedType = "PDF_COPY";
        } else {
            writeLightweightPdf(processedPath, buildPdfLines(assignment, storedName, originalPath, file, ext));
            processedType = isTextLike(ext) ? "LOCAL_TEXT_TO_PDF" : "LOCAL_LIGHTWEIGHT_PDF";
        }

        return new FileProcessingResult(
                processedName,
                processedPath.toString().replace("\\", "/"),
                processedType
        );
    }

    public Path resolveAssignmentArchiveDir(Assignment assignment) {
        Path archiveRoot = Path.of(resolveArchiveDir());
        return archiveRoot
                .resolve(cleanName(assignment.className() == null ? "未指定班级" : assignment.className()))
                .resolve(assignment.id() + "_" + cleanName(assignment.title()));
    }

    private String resolveArchiveDir() {
        if (appProperties.getArchiveDir() != null && !appProperties.getArchiveDir().isBlank()) {
            return appProperties.getArchiveDir();
        }
        return Path.of(appProperties.getUploadDir()).getParent().resolve("archives").toString();
    }

    private List<String> buildPdfLines(Assignment assignment, String storedName, Path originalPath,
                                       MultipartFile file, String ext) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("Homework archive PDF");
        lines.add("Assignment: " + ascii(assignment.title()));
        lines.add("Class: " + ascii(assignment.className()));
        lines.add("Renamed file: " + ascii(storedName));
        lines.add("Original file: " + ascii(file.getOriginalFilename()));
        lines.add("Conversion mode: local lightweight PDF conversion");
        lines.add("");

        if (isTextLike(ext)) {
            lines.add("Text preview:");
            byte[] bytes = readPreviewBytes(originalPath, 8192);
            String text = new String(bytes, StandardCharsets.UTF_8);
            for (String line : text.split("\\R")) {
                lines.add(ascii(line));
                if (lines.size() >= 46) {
                    lines.add("...");
                    break;
                }
            }
        } else {
            lines.add("The original document has been archived locally.");
            lines.add("In the cloud deployment plan this step can be replaced by a real cloud conversion API.");
        }

        return lines;
    }

    private byte[] readPreviewBytes(Path path, int limit) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length <= limit) {
            return bytes;
        }
        byte[] preview = new byte[limit];
        System.arraycopy(bytes, 0, preview, 0, limit);
        return preview;
    }

    private void writeLightweightPdf(Path target, List<String> lines) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 11 Tf\n50 800 Td\n");
        for (String line : lines) {
            for (String part : wrap(line, 86)) {
                content.append("(").append(escapePdf(part)).append(") Tj\n0 -16 Td\n");
            }
        }
        content.append("ET\n");

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII),
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.US_ASCII),
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>".getBytes(StandardCharsets.US_ASCII),
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.US_ASCII),
                ("<< /Length " + contentBytes.length + " >>\nstream\n" + content + "endstream").getBytes(StandardCharsets.US_ASCII)
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }
        int xrefOffset = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (Integer offset : offsets) {
            out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        Files.write(target, out.toByteArray());
    }

    private List<String> wrap(String line, int width) {
        String value = line == null ? "" : line;
        List<String> result = new ArrayList<>();
        if (value.isBlank()) {
            result.add("");
            return result;
        }
        for (int start = 0; start < value.length(); start += width) {
            result.add(value.substring(start, Math.min(value.length(), start + width)));
        }
        return result;
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String ascii(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (ch >= 32 && ch <= 126) {
                result.append(ch);
            } else if (Character.isWhitespace(ch)) {
                result.append(' ');
            } else {
                result.append('?');
            }
        }
        return result.toString();
    }

    private boolean isTextLike(String ext) {
        return List.of("txt", "md", "java", "py", "sql", "html", "css", "js", "json", "xml")
                .contains(ext.toLowerCase(Locale.ROOT));
    }

    private String removeExtension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }

    private String getExtension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String cleanName(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
