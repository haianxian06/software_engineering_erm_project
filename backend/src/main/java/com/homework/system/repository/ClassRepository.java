package com.homework.system.repository;

import com.homework.system.dto.ClassMemberResponse;
import com.homework.system.entity.ClassInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ClassRepository {

    private static final RowMapper<ClassInfo> CLASS_ROW_MAPPER = (rs, rowNum) -> new ClassInfo(
            rs.getLong("id"),
            rs.getString("class_name"),
            rs.getString("major"),
            rs.getString("course_name"),
            rs.getString("grade"),
            rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getInt("member_count")
    );

    private static final RowMapper<ClassMemberResponse> MEMBER_ROW_MAPPER = (rs, rowNum) -> new ClassMemberResponse(
            rs.getLong("user_id"),
            rs.getString("student_no"),
            rs.getString("work_no"),
            rs.getString("real_name"),
            rs.getString("major"),
            rs.getString("system_role"),
            rs.getString("member_role")
    );

    private final JdbcTemplate jdbcTemplate;

    public ClassRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ClassInfo> findManageableClasses(Long userId) {
        return jdbcTemplate.query("""
                select c.*,
                       coalesce(mc.member_count, 0) as member_count
                from class_info c
                join class_member cm_self on cm_self.class_id = c.id
                left join (
                    select class_id, count(*) as member_count
                    from class_member
                    group by class_id
                ) mc on mc.class_id = c.id
                where cm_self.user_id = ?
                  and cm_self.member_role in ('TEACHER', 'ADMIN')
                order by c.created_at desc
                """, CLASS_ROW_MAPPER, userId);
    }

    public Long saveClass(String className, String major, String courseName, String grade, Long createdBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into class_info(class_name, major, course_name, grade, created_by)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, className);
            ps.setString(2, major);
            ps.setString(3, courseName);
            ps.setString(4, grade == null ? "" : grade);
            ps.setLong(5, createdBy);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void addMember(Long classId, Long userId, String memberRole) {
        jdbcTemplate.update("""
                insert into class_member(class_id, user_id, member_role)
                values (?, ?, ?)
                on duplicate key update member_role = values(member_role)
                """, classId, userId, memberRole);
    }

    public void addStudentMember(Long classId, Long userId) {
        jdbcTemplate.update("""
                insert ignore into class_member(class_id, user_id, member_role)
                values (?, ?, 'STUDENT')
                """, classId, userId);
    }

    public int addStudentsByMajor(Long classId, String major) {
        return jdbcTemplate.update("""
                insert ignore into class_member(class_id, user_id, member_role)
                select ?, id, 'STUDENT'
                from sys_user
                where role = 'STUDENT' and major = ?
                """, classId, major);
    }

    public List<ClassMemberResponse> findMembers(Long classId) {
        return jdbcTemplate.query("""
                select u.id as user_id,
                       u.student_no,
                       u.work_no,
                       u.real_name,
                       u.major,
                       u.role as system_role,
                       cm.member_role
                from class_member cm
                join sys_user u on u.id = cm.user_id
                where cm.class_id = ?
                order by case cm.member_role when 'TEACHER' then 1 when 'ADMIN' then 2 else 3 end,
                         u.student_no,
                         u.work_no
                """, MEMBER_ROW_MAPPER, classId);
    }

    public List<ClassMemberResponse> findAvailableStudents(Long classId) {
        return jdbcTemplate.query("""
                select u.id as user_id,
                       u.student_no,
                       u.work_no,
                       u.real_name,
                       u.major,
                       u.role as system_role,
                       null as member_role
                from sys_user u
                where u.role = 'STUDENT'
                  and not exists (
                    select 1
                    from class_member cm
                    where cm.class_id = ? and cm.user_id = u.id
                  )
                order by u.major, u.student_no
                limit 100
                """, MEMBER_ROW_MAPPER, classId);
    }

    public int updateMemberRole(Long classId, Long userId, String memberRole) {
        return jdbcTemplate.update("""
                update class_member
                set member_role = ?
                where class_id = ? and user_id = ?
                """, memberRole, classId, userId);
    }

    public int removeMember(Long classId, Long userId) {
        return jdbcTemplate.update("""
                delete from class_member
                where class_id = ? and user_id = ?
                """, classId, userId);
    }
}
