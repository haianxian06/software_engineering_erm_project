package com.homework.system.repository;

import com.homework.system.dto.ArchivePackageRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ArchivePackageRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArchivePackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long assignmentId, String zipName, String zipPath, Long packageSize,
                     String tokenHash, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                insert into archive_package(
                    assignment_id, zip_name, zip_path, package_size, token_hash, expires_at
                )
                values (?, ?, ?, ?, ?, ?)
                """, assignmentId, zipName, zipPath, packageSize, tokenHash, expiresAt);
    }

    public Optional<ArchivePackageRecord> findByTokenHash(String tokenHash) {
        List<ArchivePackageRecord> result = jdbcTemplate.query("""
                select id, assignment_id, zip_name, zip_path, package_size, expires_at
                from archive_package
                where token_hash = ?
                limit 1
                """, (rs, rowNum) -> new ArchivePackageRecord(
                rs.getLong("id"),
                rs.getLong("assignment_id"),
                rs.getString("zip_name"),
                rs.getString("zip_path"),
                rs.getLong("package_size"),
                rs.getTimestamp("expires_at").toLocalDateTime()
        ), tokenHash);
        return result.stream().findFirst();
    }
}
