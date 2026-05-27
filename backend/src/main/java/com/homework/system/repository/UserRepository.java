package com.homework.system.repository;

import com.homework.system.entity.SystemUser;
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
public class UserRepository {

    private static final RowMapper<SystemUser> ROW_MAPPER = (rs, rowNum) -> new SystemUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("real_name"),
            rs.getString("student_no"),
            rs.getString("work_no"),
            rs.getString("major"),
            rs.getString("role"),
            rs.getObject("class_id") == null ? null : rs.getLong("class_id"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SystemUser> findById(Long id) {
        List<SystemUser> result = jdbcTemplate.query("select * from sys_user where id = ?", ROW_MAPPER, id);
        return result.stream().findFirst();
    }

    public Optional<SystemUser> findByStudentNo(String studentNo) {
        List<SystemUser> result = jdbcTemplate.query("""
                select * from sys_user
                where role = 'STUDENT' and student_no = ?
                """, ROW_MAPPER, studentNo);
        return result.stream().findFirst();
    }

    public Optional<SystemUser> findByWorkNoOrUsername(String value) {
        List<SystemUser> result = jdbcTemplate.query("""
                select * from sys_user
                where work_no = ? or username = ?
                """, ROW_MAPPER, value, value);
        return result.stream().findFirst();
    }

    public Long saveStudent(String studentNo, String realName, String major) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into sys_user(username, real_name, student_no, major, role)
                    values (?, ?, ?, ?, 'STUDENT')
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, studentNo);
            ps.setString(2, realName);
            ps.setString(3, studentNo);
            ps.setString(4, major);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        if (id != null) {
            jdbcTemplate.update("""
                    insert ignore into class_member(class_id, user_id, member_role)
                    select id, ?, 'STUDENT'
                    from class_info
                    where major = ?
                    """, id, major);
        }
        return id;
    }

    public Long saveTeacher(String workNo, String realName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into sys_user(username, real_name, work_no, role)
                    values (?, ?, ?, 'ADMIN')
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, workNo);
            ps.setString(2, realName);
            ps.setString(3, workNo);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public boolean hasManagePermission(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from class_member
                where user_id = ? and member_role in ('TEACHER', 'ADMIN')
                """, Integer.class, userId);
        return count != null && count > 0;
    }
}
