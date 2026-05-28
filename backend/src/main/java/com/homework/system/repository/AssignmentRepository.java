package com.homework.system.repository;

import com.homework.system.entity.Assignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class AssignmentRepository {

    private static final RowMapper<Assignment> ROW_MAPPER = (rs, rowNum) -> new Assignment(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getTimestamp("deadline").toLocalDateTime(),
            rs.getString("file_types"),
            rs.getInt("max_size_mb"),
            rs.getString("rename_pattern"),
            rs.getLong("class_id"),
            rs.getString("class_name"),
            rs.getString("created_by"),
            rs.getString("teacher_name"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public AssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Assignment> findAll() {
        return jdbcTemplate.query("""
                select a.*,
                       c.class_name,
                       coalesce(u.real_name, a.created_by) as teacher_name,
                       coalesce(ft.file_types, '') as file_types
                from assignment a
                join class_info c on c.id = a.class_id
                left join sys_user u on u.username = a.created_by or u.work_no = a.created_by
                left join (
                    select assignment_id,
                           group_concat(file_ext order by file_ext separator ',') as file_types
                    from assignment_file_type
                    group by assignment_id
                ) ft on ft.assignment_id = a.id
                order by a.created_at desc
                """, ROW_MAPPER);
    }

    public List<Assignment> findForStudent(Long userId) {
        return jdbcTemplate.query("""
                select a.*,
                       c.class_name,
                       coalesce(u.real_name, a.created_by) as teacher_name,
                       coalesce(ft.file_types, '') as file_types
                from assignment a
                join class_info c on c.id = a.class_id
                join class_member cm on cm.class_id = a.class_id
                left join sys_user u on u.username = a.created_by or u.work_no = a.created_by
                left join (
                    select assignment_id,
                           group_concat(file_ext order by file_ext separator ',') as file_types
                    from assignment_file_type
                    group by assignment_id
                ) ft on ft.assignment_id = a.id
                where cm.user_id = ?
                  and a.status = 'PUBLISHED'
                order by a.created_at desc
                """, ROW_MAPPER, userId);
    }

    public List<Assignment> findForManager(Long userId) {
        return jdbcTemplate.query("""
                select a.*,
                       c.class_name,
                       coalesce(u.real_name, a.created_by) as teacher_name,
                       coalesce(ft.file_types, '') as file_types
                from assignment a
                join class_info c on c.id = a.class_id
                join class_member cm on cm.class_id = a.class_id
                left join sys_user u on u.username = a.created_by or u.work_no = a.created_by
                left join (
                    select assignment_id,
                           group_concat(file_ext order by file_ext separator ',') as file_types
                    from assignment_file_type
                    group by assignment_id
                ) ft on ft.assignment_id = a.id
                where cm.user_id = ?
                  and cm.member_role in ('TEACHER', 'ADMIN')
                order by a.created_at desc
                """, ROW_MAPPER, userId);
    }

    public List<Assignment> findPublished() {
        return jdbcTemplate.query("""
                select a.*,
                       c.class_name,
                       coalesce(u.real_name, a.created_by) as teacher_name,
                       coalesce(ft.file_types, '') as file_types
                from assignment a
                join class_info c on c.id = a.class_id
                left join sys_user u on u.username = a.created_by or u.work_no = a.created_by
                left join (
                    select assignment_id,
                           group_concat(file_ext order by file_ext separator ',') as file_types
                    from assignment_file_type
                    group by assignment_id
                ) ft on ft.assignment_id = a.id
                where a.status = 'PUBLISHED'
                order by a.created_at desc
                """, ROW_MAPPER);
    }

    public Optional<Assignment> findById(Long id) {
        List<Assignment> result = jdbcTemplate.query("""
                select a.*,
                       c.class_name,
                       coalesce(u.real_name, a.created_by) as teacher_name,
                       coalesce(ft.file_types, '') as file_types
                from assignment a
                join class_info c on c.id = a.class_id
                left join sys_user u on u.username = a.created_by or u.work_no = a.created_by
                left join (
                    select assignment_id,
                           group_concat(file_ext order by file_ext separator ',') as file_types
                    from assignment_file_type
                    group by assignment_id
                ) ft on ft.assignment_id = a.id
                where a.id = ?
                """, ROW_MAPPER, id);
        return result.stream().findFirst();
    }

    public Long save(Assignment assignment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into assignment(title, description, deadline, max_size_mb, rename_pattern, class_id, created_by, status)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, assignment.title());
            ps.setString(2, assignment.description());
            ps.setObject(3, assignment.deadline());
            ps.setInt(4, assignment.maxSizeMb());
            ps.setString(5, assignment.renamePattern());
            ps.setLong(6, assignment.classId());
            ps.setString(7, assignment.createdBy());
            ps.setString(8, assignment.status());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        if (id != null) {
            saveFileTypes(id, assignment.fileTypes());
        }
        return id;
    }

    public void update(Assignment assignment) {
        jdbcTemplate.update("""
                update assignment
                set title = ?,
                    description = ?,
                    deadline = ?,
                    max_size_mb = ?,
                    rename_pattern = ?,
                    class_id = ?,
                    status = ?
                where id = ?
                """,
                assignment.title(),
                assignment.description(),
                assignment.deadline(),
                assignment.maxSizeMb(),
                assignment.renamePattern(),
                assignment.classId(),
                assignment.status(),
                assignment.id());
        jdbcTemplate.update("delete from assignment_file_type where assignment_id = ?", assignment.id());
        saveFileTypes(assignment.id(), assignment.fileTypes());
    }

    public List<String> findArtifactPaths(Long assignmentId) {
        return jdbcTemplate.queryForList("""
                select fr.storage_key
                from file_record fr
                join submission s on s.id = fr.submission_id
                where s.assignment_id = ?
                  and fr.storage_key is not null
                union all
                select fr.processed_storage_key
                from file_record fr
                join submission s on s.id = fr.submission_id
                where s.assignment_id = ?
                  and fr.processed_storage_key is not null
                union all
                select ap.zip_path
                from archive_package ap
                where ap.assignment_id = ?
                  and ap.zip_path is not null
                """, String.class, assignmentId, assignmentId, assignmentId);
    }

    public int deleteById(Long assignmentId) {
        jdbcTemplate.update("delete from archive_package where assignment_id = ?", assignmentId);
        jdbcTemplate.update("delete from submission_check_status where assignment_id = ?", assignmentId);
        jdbcTemplate.update("""
                delete fr
                from file_record fr
                join submission s on s.id = fr.submission_id
                where s.assignment_id = ?
                """, assignmentId);
        jdbcTemplate.update("""
                delete sr
                from submission_review sr
                join submission s on s.id = sr.submission_id
                where s.assignment_id = ?
                """, assignmentId);
        jdbcTemplate.update("delete from submission where assignment_id = ?", assignmentId);
        jdbcTemplate.update("delete from assignment_file_type where assignment_id = ?", assignmentId);
        return jdbcTemplate.update("delete from assignment where id = ?", assignmentId);
    }

    private void saveFileTypes(Long assignmentId, String fileTypes) {
        Set<String> extensions = Arrays.stream(fileTypes == null ? new String[0] : fileTypes.split(","))
                .map(String::trim)
                .map(value -> value.startsWith(".") ? value.substring(1) : value)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (extensions.isEmpty()) {
            extensions = Set.of("zip", "docx", "pdf");
        }

        for (String extension : extensions) {
            jdbcTemplate.update("""
                    insert into assignment_file_type(assignment_id, file_ext)
                    values (?, ?)
                    """, assignmentId, extension);
        }
    }
}
