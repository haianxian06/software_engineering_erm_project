package com.homework.system.repository;

import com.homework.system.entity.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final RowMapper<Student> ROW_MAPPER = (rs, rowNum) -> new Student(
            rs.getLong("id"),
            rs.getLong("class_id"),
            rs.getString("student_no"),
            rs.getString("real_name")
    );

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Student> findByStudentNo(String studentNo) {
        List<Student> result = jdbcTemplate.query("""
                select id, class_id, student_no, real_name
                from sys_user
                where role = 'STUDENT' and student_no = ?
                """, ROW_MAPPER, studentNo);
        return result.stream().findFirst();
    }

    public Student findOrCreateStudent(String studentNo, String realName, Long classId) {
        return findByStudentNo(studentNo).orElseGet(() -> {
            jdbcTemplate.update("""
                    insert into sys_user(username, real_name, student_no, role, class_id)
                    values (?, ?, ?, 'STUDENT', ?)
                    """, studentNo, realName, studentNo, classId);
            jdbcTemplate.update("""
                    insert ignore into class_member(class_id, user_id, member_role)
                    select ?, id, 'STUDENT' from sys_user where student_no = ?
                    """, classId, studentNo);
            return findByStudentNo(studentNo).orElseThrow();
        });
    }

    public boolean isMemberOfClass(Long studentId, Long classId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from class_member
                where user_id = ? and class_id = ?
                """, Integer.class, studentId, classId);
        return count != null && count > 0;
    }

    public List<Student> findMissingStudents(Long assignmentId, Long classId) {
        return jdbcTemplate.query("""
                select u.id, u.class_id, u.student_no, u.real_name
                from class_member cm
                join sys_user u on u.id = cm.user_id
                where cm.class_id = ?
                  and u.role = 'STUDENT'
                  and not exists (
                    select 1 from submission s
                    where s.assignment_id = ? and s.student_id = u.id and s.is_final = 1
                  )
                order by u.student_no
                """, ROW_MAPPER, classId, assignmentId);
    }

    public int countByClassId(Long classId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from class_member cm
                join sys_user u on u.id = cm.user_id
                where cm.class_id = ? and u.role = 'STUDENT'
                """, Integer.class, classId);
        return count == null ? 0 : count;
    }
}
