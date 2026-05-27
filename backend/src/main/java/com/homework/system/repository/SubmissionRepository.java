package com.homework.system.repository;

import com.homework.system.dto.SubmissionSummary;
import com.homework.system.dto.SubmissionSaveResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class SubmissionRepository {

    private static final RowMapper<SubmissionSummary> SUMMARY_ROW_MAPPER = (rs, rowNum) -> new SubmissionSummary(
            rs.getLong("submission_id"),
            rs.getString("student_no"),
            rs.getString("real_name"),
            rs.getString("stored_name"),
            rs.getString("original_name"),
            rs.getLong("file_size"),
            rs.getTimestamp("submit_time").toLocalDateTime(),
            rs.getInt("version_no"),
            rs.getString("submission_status"),
            rs.getBoolean("is_final"),
            rs.getString("processed_name"),
            rs.getString("processed_storage_key"),
            rs.getString("processed_type")
    );

    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SubmissionSaveResult saveSubmission(Long assignmentId, Long studentId) {
        Integer nextVersion = jdbcTemplate.queryForObject("""
                select coalesce(max(version_no), 0) + 1
                from submission
                where assignment_id = ? and student_id = ?
                """, Integer.class, assignmentId, studentId);

        jdbcTemplate.update("""
                update submission
                set is_final = 0,
                    status = 'REPLACED'
                where assignment_id = ? and student_id = ?
                """, assignmentId, studentId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into submission(assignment_id, student_id, status, version_no, is_final)
                    values (?, ?, 'SUBMITTED', ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, assignmentId);
            ps.setLong(2, studentId);
            ps.setInt(3, nextVersion == null ? 1 : nextVersion);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new SubmissionSaveResult(key == null ? null : key.longValue(), nextVersion == null ? 1 : nextVersion);
    }

    public void saveFileRecord(Long submissionId, String originalName, String storedName,
                               String storageKey, long fileSize, String mimeType,
                               String processedName, String processedStorageKey, String processedType) {
        jdbcTemplate.update("""
                insert into file_record(
                    submission_id, original_name, stored_name, storage_key,
                    file_size, mime_type, processed_name, processed_storage_key, processed_type
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, submissionId, originalName, storedName, storageKey, fileSize, mimeType,
                processedName, processedStorageKey, processedType);
    }

    public List<SubmissionSummary> findSummariesByAssignmentId(Long assignmentId) {
        return jdbcTemplate.query("""
                select s.id as submission_id,
                       u.student_no,
                       u.real_name,
                       f.stored_name,
                       f.original_name,
                       f.file_size,
                       s.submit_time,
                       s.version_no,
                       s.status as submission_status,
                       s.is_final,
                       f.processed_name,
                       f.processed_storage_key,
                       f.processed_type
                from submission s
                join sys_user u on s.student_id = u.id
                left join file_record f on f.submission_id = s.id
                where s.assignment_id = ? and s.is_final = 1
                order by u.student_no
                """, SUMMARY_ROW_MAPPER, assignmentId);
    }

    public Optional<SubmissionSummary> findCurrentSubmission(Long assignmentId, String studentNo) {
        List<SubmissionSummary> result = jdbcTemplate.query("""
                select s.id as submission_id,
                       u.student_no,
                       u.real_name,
                       f.stored_name,
                       f.original_name,
                       f.file_size,
                       s.submit_time,
                       s.version_no,
                       s.status as submission_status,
                       s.is_final,
                       f.processed_name,
                       f.processed_storage_key,
                       f.processed_type
                from submission s
                join sys_user u on s.student_id = u.id
                left join file_record f on f.submission_id = s.id
                where s.assignment_id = ?
                  and u.student_no = ?
                  and s.is_final = 1
                order by s.submit_time desc
                limit 1
                """, SUMMARY_ROW_MAPPER, assignmentId, studentNo);
        return result.stream().findFirst();
    }

    public List<SubmissionSummary> findHistoryByAssignmentIdAndStudentNo(Long assignmentId, String studentNo) {
        return jdbcTemplate.query("""
                select s.id as submission_id,
                       u.student_no,
                       u.real_name,
                       f.stored_name,
                       f.original_name,
                       f.file_size,
                       s.submit_time,
                       s.version_no,
                       s.status as submission_status,
                       s.is_final,
                       f.processed_name,
                       f.processed_storage_key,
                       f.processed_type
                from submission s
                join sys_user u on s.student_id = u.id
                left join file_record f on f.submission_id = s.id
                where s.assignment_id = ?
                  and u.student_no = ?
                order by s.version_no desc, s.submit_time desc
                """, SUMMARY_ROW_MAPPER, assignmentId, studentNo);
    }
}
